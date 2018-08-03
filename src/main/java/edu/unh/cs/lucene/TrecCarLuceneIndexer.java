package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarPageRepr;
import edu.unh.cs.TrecCarParagraph;
import edu.unh.cs.TrecCarRepr;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * User: dietz
 * Date: 1/4/18
 * Time: 1:23 PM
 */

/**
 * Example of how to build a lucene index of trec car paragraphs
 */
public class TrecCarLuceneIndexer {

    private static void usage() {
        System.out.println("Command line parameters: (paragraph|page|entity|ecm|aspect|names) CBOR LuceneINDEX (std|english)");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");

        if(args.length <1){
            usage();
        }

        if("--tool-version".equals(args[0])) {
            System.out.println("10");
            System.exit(0);
        }

        if (args.length < 3)
            usage();

        final String representation = args[0];
        TrecCarLuceneConfig.LuceneIndexConfig cfg = TrecCarLuceneConfig.getLuceneIndexConfig(representation);

        final String cborFile = args[1];
        final String indexPath = args[2];


        final String analyzer = (args.length >3)?args[3]:"std";


        if(cfg.isPageConfig){
            pageMode(cborFile, indexPath, cfg.getTrecCarPageRepr(), setupIndexWriter(indexPath, cfg.getIndexName(), analyzer), cfg);
        } else {
            paragraphMode(cborFile, indexPath,  cfg.getTrecCarParaRepr(), setupIndexWriter(indexPath, cfg.getIndexName(), analyzer), cfg);
        }
        System.out.println("Index written to "+indexPath+"/"+cfg.getIndexName());

        }

    private static void pageMode(String pagesCborFile, String indexPath, TrecCarPageRepr trecCarPageRepr, IndexWriter indexWriter, TrecCarLuceneConfig.LuceneIndexConfig cfg) throws IOException {
        final FileInputStream fileInputStream = new FileInputStream(new File(pagesCborFile));

        System.out.println("Creating page index in "+indexPath);

        final Iterator<Data.Page> pageIterator = DeserializeData.iterAnnotations(fileInputStream);

        int i = 1;
        while(pageIterator.hasNext()){
            final Data.Page page = pageIterator.next();
            final List<Document> docs = trecCarPageRepr.pageToLuceneDoc(page);
            indexWriter.addDocuments(docs);
            if (i % 10000 == 0) {
                System.out.print('.');
                indexWriter.commit();
            }
            i+= docs.size();
        }

        System.out.println("\n Done indexing.");

        indexWriter.commit();
        indexWriter.close();
    }

    private static void paragraphMode(String paragraphCborFile, String indexPath, TrecCarParagraph trecCarParaRepr, IndexWriter indexWriter1, TrecCarLuceneConfig.LuceneIndexConfig cfg) throws IOException {
        final FileInputStream fileInputStream2 = new FileInputStream(new File(paragraphCborFile));

        System.out.println("Creating paragraph index in "+indexPath);
        final IndexWriter indexWriter = indexWriter1;
        final Iterator<Data.Paragraph> paragraphIterator = DeserializeData.iterParagraphs(fileInputStream2);

        for (int i=1; paragraphIterator.hasNext(); i++){
            final Data.Paragraph paragraph = paragraphIterator.next();
            final Document doc = trecCarParaRepr.paragraphToLuceneDoc(paragraph);
            indexWriter.addDocument(doc);
            if (i % 10000 == 0) {
                System.out.print('.');
                indexWriter.commit();
            }
        }

        System.out.println("\n Done indexing.");

        indexWriter.commit();
        indexWriter.close();
    }


    @NotNull
    private static IndexWriter setupIndexWriter(String indexPath, String typeIndex, String analyzer) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
        Directory indexDir = FSDirectory.open(path);

        final Analyzer textAnalyzer = ("std".equals(analyzer))? new StandardAnalyzer():
                ("english".equals(analyzer)? new EnglishAnalyzer(): new StandardAnalyzer());


        final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.OutlinkIds.name(), new WhitespaceAnalyzer());
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.InlinkIds.name(), new WhitespaceAnalyzer());
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.Id.name(), new WhitespaceAnalyzer());
        final DelegatingAnalyzerWrapper queryAnalyzer = new PerFieldAnalyzerWrapper(textAnalyzer, fieldAnalyzers);

        IndexWriterConfig config = new IndexWriterConfig(queryAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(indexDir, config);
    }
}