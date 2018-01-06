package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarEntity;
import edu.unh.cs.TrecCarPage;
import edu.unh.cs.TrecCarPageRepr;
import edu.unh.cs.TrecCarParagraph;

import java.util.ArrayList;
import java.util.List;

/**
 * User: dietz
 * Date: 1/5/18
 * Time: 5:58 PM
 */
public class TrecCarLuceneConfig {

    static LuceneIndexConfig getLuceneIndexConfig(String representation) {
        LuceneIndexConfig cfg = pageConfig();

        if (representation.equals("entity")) {
            cfg = entityConfig();
        }

        if (representation.equals("paragraph")) {
            cfg = paragraphConfig();
        }
        else if (representation.equals("pages")) {
            cfg = pageConfig();
        }
        return cfg;
    }

    public static class LuceneIndexConfig {
        String representation;
        String indexName = "paragraph.lucene";
        TrecCarParagraph trecCarParaRepr = new TrecCarParagraph();
        TrecCarPageRepr trecCarPageRepr = new TrecCarPage();
        boolean isPageConfig = false;

        LuceneIndexConfig() {
        }

        public String getRepresentation() {
            return representation;
        }

        public String getIndexName() {
            return indexName;
        }

        public TrecCarParagraph getTrecCarParaRepr() {
            return trecCarParaRepr;
        }

        public TrecCarPageRepr getTrecCarPageRepr() {
            return trecCarPageRepr;
        }


        public List<String> getSearchFields() {
            final ArrayList<String> searchFields = new ArrayList<>();
            if (isPageConfig) {
                for( TrecCarPageRepr.PageField field: TrecCarPageRepr.PageField.values())
                    searchFields.add(searchFields.toString());
            }
            else {
                for( TrecCarParagraph.ParagraphField field: TrecCarParagraph.ParagraphField.values())
                    searchFields.add(searchFields.toString());
            }
            return searchFields;

        }



    }

    public static LuceneIndexConfig paragraphConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "paragraph.lucene";
        config.trecCarParaRepr = new TrecCarParagraph();
        config.isPageConfig = false;
        return config;
    }

    public static LuceneIndexConfig pageConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "page.lucene";
        config.trecCarPageRepr = new TrecCarPage();
        config.isPageConfig = true;
        return config;
    }

    public static LuceneIndexConfig entityConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "entity.lucene";
        config.trecCarPageRepr = new TrecCarEntity();
        config.isPageConfig = true;
        return config;
    }


    public static class LuceneQueryConfig {
        LuceneIndexConfig indexConfig;
        boolean outputAsRun = true;
        boolean queryAsSection = true;


        public LuceneQueryConfig(LuceneIndexConfig indexConfig, boolean outputAsRun, boolean queryAsSection) {
            this.indexConfig = indexConfig;
            this.outputAsRun = outputAsRun;
            this.queryAsSection = queryAsSection;
        }

        public LuceneIndexConfig getIndexConfig() {
            return indexConfig;
        }

        public boolean isOutputAsRun() {
            return outputAsRun;
        }

        public boolean isQueryAsSection() {
            return queryAsSection;
        }
    }




}

