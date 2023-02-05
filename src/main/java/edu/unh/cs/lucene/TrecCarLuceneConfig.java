package edu.unh.cs.lucene;

import edu.unh.cs.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for various Lucene representations.
 * @author Laura Dietz
 */
public class TrecCarLuceneConfig {

    public static LuceneIndexConfig getLuceneIndexConfig(@NotNull String representation) {
        LuceneIndexConfig cfg;

        switch (representation) {
            case "entity":
                cfg = entityConfig();
                break;
            case "paragraph":
                cfg = paragraphConfig();
                break;
            case "page":
                cfg = pageConfig();
                break;
            case "ecm":
                cfg = ecmConfig();
                break;
            case "ecmentity":
                cfg = ecmEntityConfig();
                break;
            case "aspect":
                cfg = aspectConfig();
                break;
            case "names":
                cfg = namesConfig();
                break;
            default:
                throw new UnsupportedOperationException("Unknown index type " + representation);
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

    }

    @NotNull
    public static LuceneIndexConfig paragraphConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "paragraph.lucene";
        config.trecCarRepr = new TrecCarParagraph();
        config.isPageConfig = false;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig pageConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "page.lucene";
        config.trecCarRepr = new TrecCarPage();
        config.isPageConfig = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig entityConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "entity.lucene";
        config.trecCarRepr = new TrecCarEntity();
        config.isPageConfig = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig ecmConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "ecm.lucene";
        config.trecCarRepr = new TrecCarEcm();
        config.isPageConfig = true;
        config.emitsList = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig ecmEntityConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "ecmentity.lucene";
        config.trecCarRepr = new TrecCarEcmEntity();
        config.isPageConfig = true;
        config.emitsList = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig aspectConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "aspect.lucene";
        config.trecCarRepr = new TrecCarAspect();
        config.isPageConfig = true;
        config.emitsList = true;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }

    @NotNull
    public static LuceneIndexConfig namesConfig() {
        final LuceneIndexConfig config = new LuceneIndexConfig();
        config.indexName = "names.lucene";
        config.trecCarRepr = new TrecCarNames();
        config.isPageConfig = true;
        config.emitsList = false;
        config.setSearchFields(config.getDefaultSearchFields());
        return config;
    }


    public static class LuceneQueryConfig {
        LuceneIndexConfig indexConfig;
        boolean outputAsRun;
        boolean queryAsSection;
        boolean queryPageViaSection;


        public LuceneQueryConfig(LuceneIndexConfig indexConfig, boolean outputAsRun, boolean queryAsSection, boolean queryPageViaSection) {
            this.indexConfig = indexConfig;
            this.outputAsRun = outputAsRun;
            this.queryAsSection = queryAsSection;
            this.queryPageViaSection = queryPageViaSection;
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

        public boolean isQueryPageViaSection() {
            return queryPageViaSection;
        }

        
    }




}

