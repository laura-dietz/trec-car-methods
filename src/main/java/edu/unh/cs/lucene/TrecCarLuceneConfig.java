package edu.unh.cs.lucene;

import edu.unh.cs.*;

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
        else if (representation.equals("ecm")) {
            cfg = ecmConfig();
        }
        return cfg;
    }

    public static class LuceneIndexConfig {
        String representation;
        String indexName = "paragraph.lucene";
        TrecCarRepr trecCarRepr = null;
        boolean isPageConfig = false;
        private List<String> searchFields;
        public boolean emitsList = false;

        LuceneIndexConfig() {
        }

        public String getRepresentation() {
            return representation;
        }

        public String getIndexName() {
            return indexName;
        }

        public TrecCarParagraph getTrecCarParaRepr() {
            return (TrecCarParagraph) trecCarRepr;
        }

        public TrecCarPageRepr getTrecCarPageRepr() {
            return (TrecCarPageRepr) trecCarRepr;
        }

        public TrecCarRepr getTrecCarRepr() {
            return trecCarRepr;
        }

        public List<String> getDefaultSearchFields() {
            final ArrayList<String> searchFields = new ArrayList<>();
                for( TrecCarRepr.TrecCarSearchField field: trecCarRepr.getSearchFields())
                    searchFields.add(field.toString());
            return searchFields;
        }

        public void setSearchFields(List<String> searchFields){
            this.searchFields = searchFields;
        }

        public List<String> getSearchFields() {
            return searchFields;
        }

        public boolean isPageConfig() {
            return isPageConfig;
        }

        public boolean isEmitsList() {
            return emitsList;
        }
    }

    public static LuceneIndexConfig paragraphConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "paragraph.lucene";
        config.trecCarRepr = new TrecCarParagraph();
        config.isPageConfig = false;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    public static LuceneIndexConfig pageConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "page.lucene";
        config.trecCarRepr = new TrecCarPage();
        config.isPageConfig = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    public static LuceneIndexConfig entityConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "entity.lucene";
        config.trecCarRepr = new TrecCarEntity();
        config.isPageConfig = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    public static LuceneIndexConfig ecmConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "ecm.lucene";
        config.trecCarRepr = new TrecCarEcm();
        config.isPageConfig = true;
        config.emitsList = true;
        config.setSearchFields(config.getDefaultSearchFields());
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

