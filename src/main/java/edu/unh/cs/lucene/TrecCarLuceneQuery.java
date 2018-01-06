package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarParagraph;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.unh.cs.lucene.TrecCarLuceneQuery.MyQueryBuilder.TEXT_FIELD;

/*
 * User: dietz
 * Date: 1/4/18
 * Time: 1:23 PM
 */

/**
 * Example of how to build a lucene index of trec car paragraphs
 */
public class TrecCarLuceneQuery {


    static class MyQueryBuilder {


        TrecCarParagraph trecCarParaRepr = new TrecCarParagraph();
        String paragraphIndexName = "paragraph.lucene";
        final public static String TEXT_FIELD = TrecCarParagraph.ParagraphField.Text.toString();

        private final StandardAnalyzer analyzer;
        private final List<String> searchFields;
        private List<String> tokens;

        public MyQueryBuilder(StandardAnalyzer standardAnalyzer, List<String> searchFields){
            analyzer = standardAnalyzer;
            this.searchFields = searchFields;
            tokens = new ArrayList<>(128);
        }

        public BooleanQuery toQuery(String queryStr) throws IOException {

            TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, new StringReader(queryStr));
            tokenStream.reset();
            tokens.clear();
            while (tokenStream.incrementToken()) {
                final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
                tokens.add(token);
            }
            tokenStream.end();
            tokenStream.close();
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            String searchField = TEXT_FIELD;
            for (String token : tokens) {
                booleanQuery.add(new TermQuery(new Term(searchField, token)), BooleanClause.Occur.SHOULD);
            }
            return booleanQuery.build();
        }
    }

    private static void usage() {
        System.out.println("Command line parameters: (paragraph|page|entity|edgedoc)  (section|page) (run|display) OutlineCBOR INDEX\n");
        System.exit(-1);
    }
//        System.out.println("Command line parameters: (paragraphs|pages)CBOR LuceneINDEX");


    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");

        if (args.length < 5)
            usage();

        final String representation = args[0];
        TrecCarLuceneConfig.LuceneIndexConfig icfg = TrecCarLuceneConfig.getLuceneIndexConfig(representation);

        String queryAs = args[1];
        String output = args[2];
        TrecCarLuceneConfig.LuceneQueryConfig cfg = new TrecCarLuceneConfig.LuceneQueryConfig(icfg, "run".equals(output), "section".equals(queryAs));


        final String queryCborFile = args[3];
        final String indexPath = args[4];

        System.out.println("Index loaded from "+indexPath+"/"+cfg.getIndexConfig().getIndexName());
        IndexSearcher searcher = setupIndexSearcher(indexPath, cfg.getIndexConfig().indexName);
        searcher.setSimilarity(new BM25Similarity());
        final MyQueryBuilder queryBuilder = new MyQueryBuilder(new StandardAnalyzer(), cfg.getIndexConfig().getSearchFields());



        if(cfg.queryAsSection ) {
            final FileInputStream fileInputStream3 = new FileInputStream(new File(queryCborFile));
            for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream3)) {
                if (!cfg.outputAsRun)  System.out.println("\n\nPage: " + page.getPageId());
                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                    if (!cfg.outputAsRun) {
                        System.out.println();
                        System.out.println(Data.sectionPathId(page.getPageId(), sectionPath) + "   \t " + Data.sectionPathHeadings(sectionPath));
                    }

                    final String queryStr = buildSectionQueryStr(page, sectionPath);
                    final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                    oneQuery(searcher, queryBuilder, queryStr, queryId, cfg.isOutputAsRun());
                }
            }
            System.out.println();
        }
        else if(!cfg.queryAsSection){
            final FileInputStream fileInputStream3 = new FileInputStream(new File(queryCborFile));
            for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream3)) {
                    if (!cfg.outputAsRun)  System.out.println("\n\nPage: "+page.getPageId());

                    final String queryStr = buildSectionQueryStr(page, Collections.emptyList());
                    final String queryId = page.getPageId();
                    oneQuery(searcher, queryBuilder, queryStr, queryId, cfg.isOutputAsRun());
            }
            System.out.println();
        }
    }

    private static void oneQuery(IndexSearcher searcher, MyQueryBuilder queryBuilder, String queryStr, String queryId, boolean outputAsRun) throws IOException {
        // get top 10 documents
        final BooleanQuery booleanQuery = queryBuilder.toQuery(queryStr);
        TopDocs tops = searcher.search(booleanQuery, 100);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        if(!outputAsRun) System.out.println("Found "+scoreDoc.length+" results.");

        for (int i = 0; i < scoreDoc.length; i++) {
            ScoreDoc score = scoreDoc[i];
            final Document doc = searcher.doc(score.doc); // to access stored content
            // print score and internal docid
            final String docId = doc.getField("ID").stringValue();
            final float searchScore = score.score;
            final int searchRank = i+1;

            if(!outputAsRun) {
                System.out.println(docId + " (" + score.doc + "):  SCORE " + score.score);
                // access and print content
                System.out.println("  " + doc.getField(TEXT_FIELD).stringValue());
            }

            if(outputAsRun) {
                System.out.println(queryId + " Q0 " + docId + " " + searchRank + " " + searchScore + " Lucene-BM25");
            }
        }
    }

    @NotNull
    private static IndexSearcher setupIndexSearcher(String indexPath, String typeIndex) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
        Directory indexDir = FSDirectory.open(path);
        IndexReader reader = DirectoryReader.open(indexDir);
        return new IndexSearcher(reader);
    }

    @NotNull
    private static String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
        StringBuilder queryStr = new StringBuilder();
        queryStr.append(page.getPageName());
        for (Data.Section section: sectionPath) {
            queryStr.append(" ").append(section.getHeading());
        }
        return queryStr.toString();
    }


}