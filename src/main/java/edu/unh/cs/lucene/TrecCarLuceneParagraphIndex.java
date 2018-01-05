package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarPage;
import edu.unh.cs.TrecCarParagraph;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
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
import java.util.Iterator;

/*
 * User: dietz
 * Date: 1/4/18
 * Time: 1:23 PM
 */

/**
 * Example of how to build a lucene index of trec car paragraphs
 */
public class TrecCarLuceneParagraphIndex {

    private static void usage() {
        System.out.println("Command line parameters: (paragraphs|pages) CBOR LuceneINDEX");
        System.exit(-1);
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");

        TrecCarParagraph trecCarParaRepr = new TrecCarParagraph();
        TrecCarPage trecCarPageRepr = new TrecCarPage();


        if (args.length < 3)
            usage();

        String mode = args[0];
        String indexPath = args[2];

        if (mode.equals("paragraphs")) {
            final String paragraphsFile = args[1];
            final FileInputStream fileInputStream2 = new FileInputStream(new File(paragraphsFile));

            System.out.println("Creating paragraph index in "+indexPath);
            final IndexWriter indexWriter = setupIndexWriter(indexPath, "paragraph.lucene");
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
        else if (mode.equals("pages")) {
            final String pagesFile = args[1];
            final FileInputStream fileInputStream = new FileInputStream(new File(pagesFile));

            System.out.println("Creating page index in "+indexPath);
            final IndexWriter indexWriter = setupIndexWriter(indexPath, "pages.lucene");

            final Iterator<Data.Page> paragraphIterator = DeserializeData.iterAnnotations(fileInputStream);

            for (int i=1; paragraphIterator.hasNext(); i++){
                final Data.Page page = paragraphIterator.next();
                final Document doc = trecCarPageRepr.pageToLuceneDoc(page);
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
    }


    @NotNull
    private static IndexWriter setupIndexWriter(String indexPath, String typeIndex) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
        Directory indexDir = FSDirectory.open(path);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        return new IndexWriter(indexDir, config);
    }
}