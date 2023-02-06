package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarRepr;
import edu.unh.cs.lucene.TrecCarLuceneConfig.LuceneQueryConfig;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

/*
 * User: dietz
 * Date: 1/4/18
 * Time: 1:23 PM
 */

/**
 * Query a Lucene index.
 * @author Laura Dietz
 */
public class TrecCarLuceneQuery {


    public static void usage() {
        System.out.println("Command line parameters: query (paragraph|page|entity|ecm|aspect) " +
                " (section|page|pageViaSection) (run|display) OutlineCBOR INDEX RUNFile" +
                " (sectionPath|all|subtree|title|leafHeading|interior)" +
                " (bm25|ql|default) (none|rm|ecm|ecm-rm|ecm-psg|rm1|ecm-psg1) (std|english) numResults " +
                "numRmExpansionDocs numRmExpansionTerms (killQueryEntities|none) [searchField1] [searchField2] ...\n" +
                "searchFields one of "+Arrays.toString(TrecCarRepr.TrecCarSearchField.values()));
        System.exit(-1);
    }

    private boolean parseKillQueryEntitiesArgument(String inputString) throws RuntimeException {
        if(inputString.equalsIgnoreCase("none")) return false;
        else if(inputString.equalsIgnoreCase("no")) return false;
        else if(inputString.startsWith("kill")) return true;
        else throw new RuntimeException("argument must either be killQueryEntities or none, but is \'"+inputString+"\'");
    }

    public TrecCarLuceneQuery(String representation,
                              String queryType,
                              String output,
                              String queryCborFile,
                              String indexPath,
                              String runFileName,
                              String queryModel,
                              String retrievalModel,
                              String expansionModel,
                              String analyzerStr,
                              int numResults,
                              int numRmExpansionDocs,
                              int numRmExpansionTerms,
                              String killQueryEntityIds,
                              List<String> searchFields) {

        TrecCarLuceneConfig.LuceneIndexConfig indexConfig = TrecCarLuceneConfig.getLuceneIndexConfig(representation);
        TrecCarLuceneConfig.LuceneQueryConfig cfg = new TrecCarLuceneConfig.LuceneQueryConfig(indexConfig,
                !("display".equals(output)), "section".equals(queryType), "pageViaSection".equals(queryType), 
                parseKillQueryEntitiesArgument(killQueryEntityIds));

        System.out.println("Index loaded from " + indexPath + "/" + cfg.getIndexConfig().getIndexName());
        IndexSearcher searcher = null;
        try {
            searcher = setupIndexSearcher(indexPath, cfg.getIndexConfig().indexName);
            if ("bm25".equals(retrievalModel)) {
                searcher.setSimilarity(new BM25Similarity());
            }
            else if ("ql".equals(retrievalModel)) {
                searcher.setSimilarity(new LMDirichletSimilarity(1500));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> searchFieldsUsed;
        if (searchFields == null) {
            searchFieldsUsed = cfg.getIndexConfig().getSearchFields();
        } else {
            searchFieldsUsed = searchFields;
        }

        final Analyzer queryAnalyzer = indexConfig.trecCarRepr.getAnalyzer(analyzerStr);

        final MyQueryBuilder queryBuilder = new MyQueryBuilder(queryAnalyzer, searchFieldsUsed, indexConfig.trecCarRepr );
        final QueryBuilder.QueryStringBuilder queryStringBuilder =
                ("sectionPath".equals(queryModel))? new QueryBuilder.SectionPathQueryStringBuilder() :
                        ("all".equals(queryModel) ? new QueryBuilder.OutlineQueryStringBuilder():
                                ("subtree".equals(queryModel) ? new QueryBuilder.SubtreeQueryStringBuilder():
                                        ("title".equals(queryModel) ? new QueryBuilder.TitleQueryStringBuilder():
                                                ("leafHeading".equals(queryModel) ? new QueryBuilder.LeafHeadingQueryStringBuilder():
                                                        ("interior".equals(queryModel) ? new QueryBuilder.InteriorHeadingQueryStringBuilder():
                                                                ("para".equals(queryModel) ? new QueryBuilder.ParagraphQueryStringBuilder():
                                                                        new QueryBuilder.SectionPathQueryStringBuilder()
                                                                ))))));

        doTask(queryCborFile, cfg, queryStringBuilder, searcher, queryBuilder, runFileName,
                expansionModel,numResults,numRmExpansionDocs, numRmExpansionTerms, queryModel);


    }

    private void doTask(String queryCborFile,
                        TrecCarLuceneConfig.LuceneQueryConfig cfg,
                        QueryBuilder.QueryStringBuilder queryStringBuilder,
                        IndexSearcher searcher,
                        MyQueryBuilder queryBuilder,
                        String runFileName,
                        String expansionModel,
                        int numResults,
                        int numRmExpansionDocs,
                        int numRmExpansionTerms,
                        String queryModel) {

        PrintWriter runFile = null;
        try {
            runFile = new PrintWriter(runFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedInputStream bufferedInputStream = null;
        if(cfg.isQueryAsSection() ) {
            //  #####################################
            //            Query Section
            //  #####################################


            try {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(queryCborFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            for (Data.Page page : DeserializeData.iterableAnnotations(bufferedInputStream)) {
                HashSet<String> alreadyQueried = new HashSet<>();
                //System.out.println("\n\nPage: " + page.getPageId());
                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                    System.out.println();
                    System.out.println(Data.sectionPathId(page.getPageId(), sectionPath) + "   \t " + Data.sectionPathHeadings(sectionPath));

                    final String queryStr = queryStringBuilder.buildSectionQueryStr(page, sectionPath);
                    final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                    if(!alreadyQueried.contains(queryId)) {
                        try {
                            expandedRetrievalModels(cfg, searcher, queryBuilder, runFile, queryStr, queryId,
                                    expansionModel, numResults, numRmExpansionDocs, numRmExpansionTerms, queryModel);
                            System.out.println("Query Completed: " + page.getPageId());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        alreadyQueried.add(queryId);
                    }
                }
            }
        }
        else if(cfg.isQueryPageViaSection()) {
            //  #####################################
            //            Query Page Via Section
            //  #####################################

            PrintStream debugStream = (!cfg.isOutputAsRun()?System.out:SYSTEM_NULL);
            PrintWriter noopRunFile = null;

            // compose single page query out of section-wise queries
            try {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(queryCborFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            for (Data.Page page : DeserializeData.iterableAnnotations(bufferedInputStream)) {
                final String pageQueryId = page.getPageId();
                HashSet<String> alreadyQueried = new HashSet<>();
                //System.out.println("\n\nPage: " + page.getPageId());
                List<Collection<Entry<String, Float>>> runs = new ArrayList<>();


                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                    System.out.println();
                    System.out.println(Data.sectionPathId(page.getPageId(), sectionPath) + "   \t " + Data.sectionPathHeadings(sectionPath));

                    final String queryStr = queryStringBuilder.buildSectionQueryStr(page, sectionPath);
                    final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                    if(!alreadyQueried.contains(queryId)) {
                        try {
                            // todo collect ranking output from expandedRetrievalModels
                            runs.add(expandedRetrievalModels(cfg, searcher, queryBuilder, noopRunFile, queryStr, queryId,
                                    expansionModel, numResults, numRmExpansionDocs, numRmExpansionTerms, queryModel));
                            System.out.println("Section Query Completed: " + page.getPageId());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        alreadyQueried.add(queryId);
                    }
                }

                try {
                    writeMergedRankings(cfg, numResults, runs, pageQueryId, runFile, debugStream, queryModel );
                    System.out.println("Page Query Completed: " + page.getPageId()+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            //  #####################################
            //            Query Page
            //  #####################################


            try {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(queryCborFile));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            HashSet<String> alreadyQueried = new HashSet<>();
            for (Data.Page page : DeserializeData.iterableAnnotations(bufferedInputStream)) {

                final String queryStr = queryStringBuilder.buildSectionQueryStr(page, Collections.emptyList());
                final String queryId = page.getPageId();
                if(!alreadyQueried.contains(queryId)) {
                    try {
                        expandedRetrievalModels(cfg, searcher, queryBuilder, runFile, queryStr, queryId,
                                expansionModel, numResults, numRmExpansionDocs, numRmExpansionTerms, queryModel);
                        System.out.println("Query Completed: " + page.getPageId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    alreadyQueried.add(queryId);
                }
            }
        } 

        System.out.println();

        System.out.println("Written to "+ runFileName);
        assert runFile != null;
        runFile.close();
    }

    private void writeMergedRankings(LuceneQueryConfig cfg,
                                     int numResults,
                                     @NotNull List<Collection<Entry<String, Float>>> runs, 
                                     String queryId,
                                     PrintWriter runFile,
                                    //  @NotNull ScoreDoc[] scoreDoc,
                                     PrintStream debugStream,
                                     String queryModel ) throws IOException {

        Map<String, Float> accum = new HashMap<>();

        // System.out.println("\n Empty accum");
        for(Collection<Entry<String,Float>> run: runs){
            for (Entry<String,Float> entry: run){
                accum.compute(entry.getKey(), (t, oldV) ->
                        (oldV==null)? entry.getValue() : (oldV + entry.getValue())
                );
            }

            // // Debug accum
            // for(Map.Entry<String,Float> entry : accum.entrySet()){
            //     System.out.println( entry.getKey()+ "\t" + entry.getValue());
            // }
            // System.out.println("\n");
        }
        

        ArrayList<Map.Entry<String, Float>> allAccum = new ArrayList<>(accum.entrySet());
        allAccum.sort((kv1, kv2) -> {
            return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
        });

        List<Map.Entry<String, Float>> expansionTerms = allAccum.subList(0, Math.min(numResults, allAccum.size()));
        // System.out.println("");
        // for (Map.Entry<String,Float> entry: expansionTerms){
        //     System.out.println(entry.getKey() + "\t" + entry.getValue());
        // }




        HashSet<String> alreadyReturned = new HashSet<>();

        int searchRank = 0; // increment first thing
        for (Map.Entry<String,Float> entry: expansionTerms){
            searchRank = searchRank+1;
            final Float searchScore = entry.getValue();
            final String docId = entry.getKey();

            System.out.println(entry.getKey() + "\t" + entry.getValue());

            if(!alreadyReturned.contains(docId)){
                debugStream.println(docId + " (" + searchRank + "):  SCORE " + searchScore);
                runFile.println(queryId + " Q0 " + docId + " " + searchRank + " " + searchScore + " Lucene-"+ queryModel );
                }
            alreadyReturned.add(docId);
        }            
    }



    


    private final static PrintStream SYSTEM_NULL = new PrintStream(new OutputStream() {
        public void write(int b) {
            // DO NOTHING
        }
    });
    private final static PrintWriter SYSTEM_NULL_WRITER = new PrintWriter(SYSTEM_NULL);

    public static class MyQueryBuilder {

        private final Analyzer analyzer;
        private final List<String> searchFields;
        private final TrecCarRepr trecCarRepr;
        private final List<String> tokens;
        private final String textSearchField;
        private final String entitySearchField;

        public MyQueryBuilder(Analyzer analyzer, @NotNull List<String> searchFields, TrecCarRepr trecCarRepr){
            this.analyzer = analyzer;
            this.searchFields = searchFields;
            if(searchFields.size()>20) System.err.println("Warning: searching more than 20 fields, this may exceed the allowable number of 1024 boolean clauses.");
            textSearchField = trecCarRepr.getTextField().toString();
            entitySearchField = trecCarRepr.getEntityField().toString();
            this.trecCarRepr = trecCarRepr;
            tokens = new ArrayList<>(64);
        }

        public void addTokens(String content, Float weight, Map<String,Float> wordFreqs) throws IOException {
            TokenStream tokenStream = analyzer.tokenStream(textSearchField, new StringReader(content));
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
                wordFreqs.compute(token, (t, oldV) ->
                                         (oldV==null)? weight : oldV + weight
                );
            }
            tokenStream.end();
            tokenStream.close();
        }

        /**
         * RM3-style query expansion.
         * @param queryStr Un-tokenized query text
         * @param relevanceModel  already tokenized, with weights
         * @return BooleanQuery
         */
        public BooleanQuery toRm3Query(String queryStr, List<Map.Entry<String, Float>> relevanceModel) throws IOException {
            // builder for the query
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            // this is a field-specific tokenizer
            tokenizeQuery(queryStr, textSearchField, tokens);

            // this is already taken care of by the relevance model.
            // // original query terms with boost 1.0
            // for (String searchField : this.searchFields) {
            //     for (String token : tokens) {
            //         booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)),1.0f), BooleanClause.Occur.SHOULD);
            //     }
            // }

            // add query and expansion terms
            for (String searchField : this.searchFields) {
                for (Map.Entry<String, Float> stringFloatEntry : relevanceModel.subList(0, Math.min(relevanceModel.size(), (64-tokens.size())))) {
                    String token = stringFloatEntry.getKey();
                    float weight = stringFloatEntry.getValue();
                    booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)),weight), BooleanClause.Occur.SHOULD);
                }
            }

            return booleanQuery.build();
        }

        /**
         * Tokenizes the query and stores results in (the passed in) `tokens` -- Not thread safe!
         * */
        private void tokenizeQuery(String queryStr, String textSearchField, @NotNull List<String> tokens) throws IOException {
            TokenStream tokenStream = analyzer.tokenStream(textSearchField, new StringReader(queryStr));
            tokenStream.reset();
            tokens.clear();
            while (tokenStream.incrementToken() && tokens.size() < 64) {
                final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
                tokens.add(token);
            }
            tokenStream.end();
            tokenStream.close();
        }

        public BooleanQuery toEntityRmQuery(String queryStr, List<Map.Entry<String, Float>> entityRelevanceModel, boolean omitQueryTerms) throws IOException {


            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            if(!omitQueryTerms) {
                tokenizeQuery(queryStr, textSearchField, tokens);

                for (String searchField : this.searchFields) {
                    for (String token : tokens) {
                        booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)), 1.0f), BooleanClause.Occur.SHOULD);
                    }
                }
            }

            // add Entity RM terms
            for (String entitySearchField : Collections.singletonList(this.entitySearchField)) {
                for (Map.Entry<String, Float> stringFloatEntry : entityRelevanceModel.subList(0, Math.min(entityRelevanceModel.size(), (64-tokens.size())))) {
                  List<String> entityTokens = new ArrayList<>();
                  tokenizeQuery(stringFloatEntry.getKey(), entitySearchField, entityTokens);
                  for(String entity: entityTokens) {
                      float weight = stringFloatEntry.getValue();
                        booleanQuery.add(new BoostQuery(new TermQuery(new Term(entitySearchField, entity)),weight), BooleanClause.Occur.SHOULD);
                    }
                }
            }

            return booleanQuery.build();
        }


        public BooleanQuery toQuery(String queryStr) throws IOException {

            tokenizeQuery(queryStr, textSearchField, tokens);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            for (String searchField : this.searchFields) {
                for (String token : tokens) {
                    booleanQuery.add(new TermQuery(new Term(searchField, token)), BooleanClause.Occur.SHOULD);
                }
            }


            return booleanQuery.build();
        }


    }

    static Collection<Entry<String, Float>> expandedRetrievalModels(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                        IndexSearcher searcher,
                                        MyQueryBuilder queryBuilder,
                                        PrintWriter runFile,
                                        String queryStr,
                                        String queryId,
                                        String expansionModel,
                                        int numResults,
                                        int numRmExpansionDocs,
                                        int numRmExpansionTerms,
                                        String queryModel) throws IOException {
        ScoreDoc[] run = null;

        PrintStream debugStream = (!cfg.isOutputAsRun()?System.out:SYSTEM_NULL);
        if ("ecm-psg".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(cfg, searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel);
            run =  ecmPsgRanking(cfg, searcher, queryId, queryBuilder, queryStr, runFile, scoreDocs, debugStream, numRmExpansionDocs, numRmExpansionTerms,  numResults, queryModel, false);
        } else if ("ecm-psg1".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(cfg, searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel);
            run = ecmPsgRanking(cfg, searcher, queryId, queryBuilder, queryStr, runFile, scoreDocs, debugStream, numRmExpansionDocs, numRmExpansionTerms,  numResults, queryModel, true);
        } else if ("ecm-rm".equals(expansionModel)){
            final ScoreDoc[] scoreDocs = oneExpandedQuery(cfg, searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, false );
            return ecmRanking(cfg, searcher, queryId, runFile, scoreDocs, debugStream, numResults, numRmExpansionDocs,  queryModel);
        } else if ("ecm".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(cfg, searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel);
            return ecmRanking(cfg, searcher, queryId, runFile, scoreDocs, debugStream, numResults, numRmExpansionDocs, queryModel);
        } else if ("rm".equals(expansionModel)){
            run =  oneExpandedQuery(cfg, searcher, queryBuilder, queryStr, queryId, runFile, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, false);
        } else if ("rm1".equals(expansionModel)){
            run =  oneExpandedQuery(cfg, searcher, queryBuilder, queryStr, queryId, runFile, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, true);
        } else if ("none".equals(expansionModel)){
            run =  oneQuery(cfg, searcher, queryBuilder, queryStr, queryId, runFile, debugStream, numResults, queryModel);
        } else {
            System.out.println("Warning: expansion model "+ expansionModel +" not known.");
            run =  oneQuery(cfg, searcher, queryBuilder, queryStr, queryId, runFile, debugStream, numResults, queryModel);
        }

        Map<String, Float> clonedRun;

        clonedRun = accumulateEntries(numResults, run, docInt -> getDocId(searcher, docInt), new HashMap<>());

        return clonedRun.entrySet();
    }


    @NotNull
    private static List<Map.Entry<String, Float>> relevanceModel(@NotNull IndexSearcher searcher,
                                                                 @NotNull MyQueryBuilder queryBuilder,
                                                                 String queryStr,
                                                                 int takeKDocs,
                                                                 int takeKTerms,
                                                                 String expansionField,
                                                                 boolean omitQueryTerms) throws IOException {
        final BooleanQuery booleanQuery = queryBuilder.toQuery(queryStr);

        // feedback run only fetch top k documents
        TopDocs tops = searcher.search(booleanQuery, takeKDocs);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        final Map<String, Float> wordFreq = new HashMap<>();

        // add query terms with boost 1.0

        if(!omitQueryTerms) {
            queryBuilder.addTokens(queryStr, 1.0f, wordFreq);
        }

        // guess if we have log scores...
        boolean useLog = false;
        for (ScoreDoc score : scoreDoc) {
            if (score.score < 0.0) {
                useLog = true;
                break;
            }
        }

        // compute score normalizer
        double normalizer = 0.0;
        for (ScoreDoc score : scoreDoc) {
            if (useLog) normalizer += Math.exp(score.score);
            else normalizer += score.score;
        }
        if (useLog) normalizer = Math.log(normalizer);

        for (ScoreDoc score : scoreDoc) {
            double weight = useLog ? (score.score - normalizer) : (score.score / normalizer);
            String docContents = searcher.doc(score.doc).get(expansionField);
            queryBuilder.addTokens(docContents, (float) weight, wordFreq);
        }

        ArrayList<Map.Entry<String, Float>> allWordFreqs = new ArrayList<>(wordFreq.entrySet());
        allWordFreqs.sort((kv1, kv2) -> {
            return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
        });

        List<Map.Entry<String, Float>> expansionTerms = allWordFreqs.subList(0, Math.min(takeKTerms, allWordFreqs.size()));

        // System.out.println("RM3 Expansions for \""+queryStr+ "\": "+ expansionTerms);
        return expansionTerms;
    }


    public interface FetchEntries{
        Iterable<String> entries(Integer docInt) throws IOException;
    }
    @NotNull
    private static List<Map.Entry<String, Float>> marginalizeFreq(int takeKDocs,
                                                                  int takeKTerms,
                                                                  @NotNull ScoreDoc[] scoreDocAll,
                                                                  FetchEntries fetchEntries) throws IOException {
        Map<String, Float> wordFreq = accumulateEntries(takeKDocs, scoreDocAll, fetchEntries, new HashMap<>());

        ArrayList<Map.Entry<String, Float>> allWordFreq = new ArrayList<>(wordFreq.entrySet());
        allWordFreq.sort((kv1, kv2) -> {
            return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
        });

        List<Map.Entry<String, Float>> expansionTerms = allWordFreq.subList(0, Math.min(takeKTerms, allWordFreq.size()));
        System.out.println("");
        for (Map.Entry<String,Float> term: expansionTerms){
            System.out.println(term.getKey() + "\t" + term.getValue());
        }
        return expansionTerms;
    }

    private static Map<String, Float> accumulateEntries(int takeKDocs, ScoreDoc[] scoreDocAll, FetchEntries fetchEntries,  Map<String, Float> wordFreq) throws IOException {

        
        //private static List<Map.Entry<String, Float>> marginalizeFreqs(int takeKDocs, int takeKTerms, ScoreDoc[] scoreDocAll, FetchEntries fetchEntries, Map<String, Float> wordFreqs ) throws IOException {
            // only analyze the first takeKDocs of ranking
            ScoreDoc[] scoreDoc = Arrays.copyOfRange(scoreDocAll, 0, takeKDocs);
            // guess if we have log scores...
            boolean useLog = false;
            for (ScoreDoc score : scoreDoc) {
                if (score == null) {
                    continue;
                }
                if (score.score < 0.0) {
                    useLog = true;
                    break;
                }
            }

            // compute score normalizer
            double normalizer = 0.0;
            for (ScoreDoc score : scoreDoc) {
                if (score == null) {
                    continue;
                }
                if (useLog) normalizer += Math.exp(score.score);
                else normalizer += score.score;
            }
            if (useLog) normalizer = Math.log(normalizer);

            for (ScoreDoc score : scoreDoc) {
                if (score == null) {
                    continue;
                }
                Double weight = useLog ? (score.score - normalizer) : (score.score / normalizer);
                for ( String entry: fetchEntries.entries(score.doc) ) {
                    wordFreq.compute(entry, (t, oldV) ->
                            (oldV==null)? weight.floatValue() : (oldV + weight.floatValue())
                    );
                }
            }

            return wordFreq;
    }



    @NotNull
    private static ScoreDoc[] oneExpandedQuery(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                               IndexSearcher searcher,
                                               @NotNull MyQueryBuilder queryBuilder,
                                               String queryStr,
                                               String queryId,
                                               PrintWriter runFile,
                                               PrintStream debugStream,
                                               String expansionField,
                                               String queryModel,
                                               int numRmExpansionDocs,
                                               int numResults,
                                               int numRmExpansionTerms,
                                               boolean omitQueryTerms) throws IOException {

        final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;

        final List<Map.Entry<String, Float>> relevanceModel = relevanceModel(searcher, queryBuilder, queryStr, numRmExpansionDocs, numRmExpansionTerms, expansionField, omitQueryTerms);
        final BooleanQuery booleanQuery = queryBuilder.toRm3Query(queryStr, relevanceModel);

        TopDocs tops = searcher.search(booleanQuery, numResults);
        ScoreDoc[] scoreDoc = tops.scoreDocs;


        outputQueryResults(cfg, searcher, queryId, runFile, trecCarRepr, scoreDoc, debugStream, queryModel);
        return scoreDoc;
    }

    private static ScoreDoc[] oneQuery(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                       @NotNull IndexSearcher searcher,
                                       @NotNull MyQueryBuilder queryBuilder,
                                       String queryStr,
                                       String queryId,
                                       PrintWriter runFile,
                                       PrintStream debugStream,
                                       int numResults,
                                       String queryModel) throws IOException {

        final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;
        final BooleanQuery booleanQuery = queryBuilder.toQuery(queryStr);
        TopDocs tops = searcher.search(booleanQuery, numResults);
        ScoreDoc[] scoreDoc = tops.scoreDocs;


        outputQueryResults(cfg, searcher, queryId, runFile, trecCarRepr, scoreDoc, debugStream, queryModel);

        return tops.scoreDocs;
    }

    private static List<Entry<String, Float>> ecmRanking(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                   IndexSearcher searcher,
                                   String queryId,
                                   PrintWriter runFile,
                                   ScoreDoc[] scoreDoc,
                                   PrintStream debugStream,
                                   int numResults,
                                   int takeKDocs,
                                   String queryModel) throws IOException {

        List<Map.Entry<String, Float>> expansionEntities = marginalizeFreq(takeKDocs,
                numResults, scoreDoc, docInt -> getEntityIds(cfg, searcher, docInt, queryId) );

        if(runFile!=null){
            HashSet<String> alreadyReturned = new HashSet<>();
            if(cfg.isKillQueryEntityIds()) alreadyReturned.add(queryId); // never output the query id
            int rank = 1;
            for (Map.Entry<String, Float> expansionEntity : expansionEntities) {

                final String entityId = expansionEntity.getKey();
                final Float score = expansionEntity.getValue();

                if(!alreadyReturned.contains(entityId)) {
                    debugStream.println(entityId + " (" + rank + "):  SCORE " + score);
                    runFile.println(queryId + " Q0 " + entityId + " " + rank + " " + score + " Lucene-ECM-" + queryModel );
                    rank ++;
                }

                alreadyReturned.add(entityId);

            }
        }

        return expansionEntities;
    }


    @NotNull
    private static Iterable<String> getDocId(@NotNull IndexSearcher searcher, Integer docInt) throws IOException {
        final Document doc = searcher.doc(docInt); // to access stored content
        String docId = doc.getField(TrecCarRepr.TrecCarSearchField.Id.name()).stringValue();
        return Collections.singletonList(docId);
    }


    @NotNull
    private static Iterable<String> getEntityIds(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                                 @NotNull IndexSearcher searcher, Integer docInt, String queryId) throws IOException {
        final Document doc = searcher.doc(docInt); // to access stored content
        final IndexableField outLinks = doc.getField(TrecCarRepr.TrecCarSearchField.OutlinkIds.name());
        final IndexableField inLinks = doc.getField(TrecCarRepr.TrecCarSearchField.InlinkIds.name());

        ArrayList<String> result = new ArrayList<>();
        if(outLinks!=null ) {
            final String[] outLinkIds = outLinks.stringValue().split("\n");
            List<String> outlinks = Arrays.asList(outLinkIds);
            if(cfg.isKillQueryEntityIds()) outlinks.remove(queryId);
            result.addAll(outlinks);
        } else if(inLinks != null) {
            final String[] inLinkIds = inLinks.stringValue().split("\n");
            List<String> inlinks = Arrays.asList(inLinkIds);
            if(cfg.isKillQueryEntityIds())  inlinks.remove(queryId);
            result.addAll(inlinks);
        }

        result.removeIf(String::isEmpty);
        return result;
    }

    private static ScoreDoc[] ecmPsgRanking(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                      IndexSearcher searcher,
                                      String queryId,
                                      MyQueryBuilder queryBuilder,
                                      String queryStr,
                                      PrintWriter runFile,
                                      ScoreDoc[] scoreDoc,
                                      PrintStream debugStream,
                                      int takeKDocs,
                                      int takeKTerms,
                                      int numResults,
                                      String queryModel,
                                      boolean omitQueryTerms ) throws IOException {
        // Map<String, Float> entityFreq = new HashMap<>();

            List<Map.Entry<String, Float>> expansionEntities = marginalizeFreq(takeKDocs, takeKTerms, scoreDoc, 
                                                                               docInt -> getEntityIds(cfg, searcher, docInt, queryId)
            );


            final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;

            final BooleanQuery booleanQuery = queryBuilder.toEntityRmQuery(queryStr, expansionEntities, omitQueryTerms);

            TopDocs tops = searcher.search(booleanQuery, numResults);
            ScoreDoc[] scoreDoc2 = tops.scoreDocs;


            if(runFile!=null){
                outputQueryResults(cfg, searcher, queryId, runFile, trecCarRepr, scoreDoc2, debugStream, queryModel);
            }
            return tops.scoreDocs;
        
    }

    private static void outputQueryResults(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
                                           @NotNull IndexSearcher searcher,
                                           @NotNull String queryId,
                                           PrintWriter runFile,
                                           @NotNull TrecCarRepr trecCarRepr,
                                           @NotNull ScoreDoc[] scoreDoc,
                                           PrintStream debugStream,
                                           @NotNull String queryModel) throws IOException 
    {
        if (runFile != null){

            HashSet<String> alreadyReturned = new HashSet<>();
            if(cfg.isKillQueryEntityIds()) alreadyReturned.add(queryId); 
            for (int i = 0; i < scoreDoc.length; i++) {
                ScoreDoc score = scoreDoc[i];
                final Document doc = searcher.doc(score.doc); // to access stored content
                // print score and internal docid
                final String docId = doc.getField(trecCarRepr.getIdField().name()).stringValue();
                final float searchScore = score.score;
                final int searchRank = i+1;


                if(!alreadyReturned.contains(docId)){
                    debugStream.println(docId + " (" + searchRank + "):  SCORE " + score.score);
                    debugStream.println("  " + doc.getField(trecCarRepr.getTextField().name()).stringValue());
                    debugStream.println("  " + doc.getField(trecCarRepr.getEntityField().name()).stringValue());
                    //debugStream.println("  " + doc.getField(trecCarRepr.getEntityField().name()).stringValue());
                    runFile.println(queryId + " Q0 " + docId + " " + searchRank + " " + searchScore + " Lucene-"+ queryModel );
                }
                alreadyReturned.add(docId);
            }
        }
    }

    @NotNull
    static IndexSearcher setupIndexSearcher(String indexPath, String typeIndex) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
        Directory indexDir = FSDirectory.open(path);
        IndexReader reader = DirectoryReader.open(indexDir);
        return new IndexSearcher(reader);
    }


}