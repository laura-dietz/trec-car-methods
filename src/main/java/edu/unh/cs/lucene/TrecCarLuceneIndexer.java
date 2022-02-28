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

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.StreamSupport;

/*
 * ==========================================
 * Original Author: Laura Dietz
 * Date: 1/4/2018
 * ==========================================
 *
 * ==========================================
 * Modified By: Shubham Chatterjee
 * Date: 2/5/2022
 * ==========================================
 */

/**
 * Builds a Lucene index representing a paragraph, page, entity, or top-level section (aspect) from Wikipedia.
 * @author Laura Dietz
 * @version 0.16
 */
final public class TrecCarLuceneIndexer {

    public static void usage() {
        System.out.println("Command line parameters: (paragraph|page|entity|ecm|aspect|names) CBOR LuceneINDEX (std|english)");
        System.exit(-1);
    }

    public TrecCarLuceneIndexer(String representation,
                                String cborFile,
                                String indexPath,
                                String analyzer) {

        TrecCarLuceneConfig.LuceneIndexConfig cfg = TrecCarLuceneConfig.getLuceneIndexConfig(representation);

        if(cfg.isPageConfig){
            try {
                pageMode(cborFile, indexPath, cfg.getTrecCarPageRepr(), setupIndexWriter(indexPath, cfg.getIndexName(), analyzer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                paragraphMode(cborFile, indexPath,  cfg.getTrecCarParaRepr(), setupIndexWriter(indexPath, cfg.getIndexName(), analyzer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Index written to "+indexPath+"/"+cfg.getIndexName());

    }

    private void pageMode(String cborFile,
                                 String indexPath,
                                 TrecCarPageRepr trecCarPageRepr,
                                 IndexWriter indexWriter) throws IOException {

        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(cborFile));

        System.out.println("Creating page index in "+indexPath);

        final Iterator<Data.Page> pageIterator = DeserializeData.iterAnnotations(bufferedInputStream);

        for (int i = 1; pageIterator.hasNext(); i++) {
            final Data.Page page = pageIterator.next();
            //System.out.println("PageId: " + page.getPageId() + " " + "WikiDataQid: " + page.getPageMetadata().getWikiDataQid());
            final List<Document> docs = trecCarPageRepr.pageToLuceneDoc(page);
            indexWriter.addDocuments(docs);
            if (i % 10000 == 0) {
                System.out.print('.');
                indexWriter.commit();
            }
        }
        System.out.println("\n Done indexing.");

        indexWriter.commit();
        indexWriter.close();
    }

//    private void paragraphMode(String cborFile,
//                               String indexPath,
//                               TrecCarParagraph trecCarParaRepr,
//                               IndexWriter indexWriter) throws IOException {
//
//
//        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(cborFile));
//        List<String> seen = new ArrayList<>();
//
//        System.out.println("Creating paragraph index in "+indexPath);
//        final Iterator<Data.Page> pageIterator = DeserializeData.iterAnnotations(bufferedInputStream);
//
//        for (int i = 1; pageIterator.hasNext(); i++){
//            final Data.Page page = pageIterator.next();
//            List<Data.Paragraph> paragraphs = new ArrayList<>();
//            pageParagraphs(page, paragraphs);
//            for (Data.Paragraph paragraph : paragraphs) {
//                if (!seen.contains(paragraph.getParaId())) {
//                    // Check for duplicates
//                    // Only add paragraphs not yet added
//                    seen.add(paragraph.getParaId());
//                    final Document doc = trecCarParaRepr.paragraphToLuceneDoc(paragraph);
//                    indexWriter.addDocument(doc);
//                    if (i % 10000 == 0) {
//                        System.out.print('.');
//                        indexWriter.commit();
//                    }
//                }
//            }
//        }
//
//        System.out.println("\n Done indexing.");
//
//        indexWriter.commit();
//        indexWriter.close();
//    }

    private static void paragraphMode(String paragraphCborFile,
                                      String indexPath,
                                      TrecCarParagraph trecCarParaRepr,
                                      IndexWriter indexWriter) throws IOException {
        final FileInputStream fileInputStream2 = new FileInputStream(paragraphCborFile);

        System.out.println("Creating paragraph index in "+indexPath);
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


//    private void pageParagraphs(@NotNull Data.Page page, List<Data.Paragraph> paragraphs) {
//        for(Data.PageSkeleton skeleton: page.getSkeleton()){
//            if (skeleton instanceof Data.Section) {
//                sectionParagraphs((Data.Section) skeleton, paragraphs);
//            } else if (skeleton instanceof Data.Para) {
//                paragraphs.add(((Data.Para) skeleton).getParagraph());
//            }
//        }
//    }
//
//    private void sectionParagraphs(@NotNull Data.Section section, List<Data.Paragraph> paragraphs) {
//        for (Data.PageSkeleton skeleton: section.getChildren()) {
//            if (skeleton instanceof Data.Section) {
//                sectionParagraphs((Data.Section) skeleton, paragraphs);
//            } else if (skeleton instanceof Data.Para) {
//                paragraphs.add(((Data.Para) skeleton).getParagraph());
//            }
//        }
//    }


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
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.WikiDataQId.name(), new WhitespaceAnalyzer());
        final DelegatingAnalyzerWrapper queryAnalyzer = new PerFieldAnalyzerWrapper(textAnalyzer, fieldAnalyzers);

        IndexWriterConfig config = new IndexWriterConfig(queryAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(indexDir, config);
    }
}