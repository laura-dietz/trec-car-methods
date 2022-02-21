package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarRepr;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import me.tongfei.progressbar.ProgressBar;
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
                              List<String> searchFields) {

        TrecCarLuceneConfig.LuceneIndexConfig indexConfig = TrecCarLuceneConfig.getLuceneIndexConfig(representation);
        TrecCarLuceneConfig.LuceneQueryConfig cfg = new TrecCarLuceneConfig.LuceneQueryConfig(indexConfig,
                !("display".equals(output)), "section".equals(queryType));

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
        if(cfg.queryAsSection ) {
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
                            System.out.println("Done: " + page.getPageId());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        alreadyQueried.add(queryId);
                    }
                }
            }
        }
        else {
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
                        System.out.println("Done: " + page.getPageId());
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


            tokenizeQuery(queryStr, textSearchField, tokens);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            for (String searchField : this.searchFields) {
                for (String token : tokens) {
                    booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)),1.0f), BooleanClause.Occur.SHOULD);
                }
            }

            // add RM3 terms
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

    public static void usage() {
        System.out.println("Command line parameters: (paragraph|page|entity|ecm|aspect) " +
                " (section|page) (run|display) OutlineCBOR INDEX RUNFile" +
                " (sectionPath|all|subtree|title|leafHeading|interior)" +
                " (bm25|ql|default) (none|rm|ecm|ecm-rm|ecm-psg|rm1|ecm-psg1) (std|english) numResults " +
                "numRmExpansionDocs numEcmExpansionDocs numRmExpansionTerms [searchField1] [searchField2] ...\n" +
                "searchFields one of "+Arrays.toString(TrecCarRepr.TrecCarSearchField.values()));
        System.exit(-1);
    }

    static void expandedRetrievalModels(@NotNull TrecCarLuceneConfig.LuceneQueryConfig cfg,
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

        PrintStream debugStream = (!cfg.isOutputAsRun()?System.out:SYSTEM_NULL);
        if ("ecm-psg".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel); // change back
            ecmPsgRanking(searcher, queryId, queryBuilder, queryStr, runFile, scoreDocs, debugStream, numRmExpansionTerms, numResults, queryModel, false);
        } else if ("ecm-psg1".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel); // change back
            ecmPsgRanking(searcher, queryId, queryBuilder, queryStr, runFile, scoreDocs, debugStream, numRmExpansionTerms, numResults, queryModel, true);
        } else if ("ecm-rm".equals(expansionModel)){
            final ScoreDoc[] scoreDocs = oneExpandedQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, false );
            ecmRanking(searcher, queryId, runFile, scoreDocs, debugStream, numResults, queryModel);
        } else if ("ecm".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, numResults, queryModel); // change back
            ecmRanking(searcher, queryId, runFile, scoreDocs, debugStream, numResults, queryModel);
        } else if ("rm".equals(expansionModel)){
            oneExpandedQuery(searcher, queryBuilder, queryStr, queryId, runFile, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, false);
        } else if ("rm1".equals(expansionModel)){
            oneExpandedQuery(searcher, queryBuilder, queryStr, queryId, runFile, debugStream, queryBuilder.trecCarRepr.getTextField().name(), queryModel, numRmExpansionDocs, numResults, numRmExpansionTerms, true);
        } else if ("none".equals(expansionModel)){
            oneQuery(searcher, queryBuilder, queryStr, queryId, runFile, debugStream, numResults, queryModel);
        } else {
            System.out.println("Warning: expansion model "+ expansionModel +" not known.");
            oneQuery(searcher, queryBuilder, queryStr, queryId, runFile, debugStream, numResults, queryModel);
        }
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
        TopDocs tops = searcher.search(booleanQuery, takeKDocs);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        final Map<String, Float> wordFreq = new HashMap<>();

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
    private static List<Map.Entry<String, Float>> marginalizeFreq(int takeKTerms,
                                                                  @NotNull ScoreDoc[] scoreDoc,
                                                                  FetchEntries fetchEntries,
                                                                  Map<String, Float> wordFreq ) throws IOException {
        // guess if we have log scores...
        boolean useLog = false;
        for (ScoreDoc score : scoreDoc) {
            if (score.score < 0.0) {
                useLog = true;
                break;
            }
            break;
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
            for ( String entry: fetchEntries.entries(score.doc) ) {
                wordFreq.compute(entry, (t, oldV) ->
                        (oldV==null)? (float) weight : (oldV + (float)weight)
                );
            }
        }

        ArrayList<Map.Entry<String, Float>> allWordFreq = new ArrayList<>(wordFreq.entrySet());
        allWordFreq.sort((kv1, kv2) -> {
            return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
        });

        return allWordFreq.subList(0, Math.min(takeKTerms, allWordFreq.size()));
    }



    @NotNull
    private static ScoreDoc[] oneExpandedQuery(IndexSearcher searcher,
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


        outputQueryResults(searcher, queryId, runFile, trecCarRepr, scoreDoc, debugStream, queryModel);
        return scoreDoc;
    }

    private static ScoreDoc[] oneQuery(@NotNull IndexSearcher searcher,
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


        outputQueryResults(searcher, queryId, runFile, trecCarRepr, scoreDoc, debugStream, queryModel);

        return tops.scoreDocs;
    }

    private static void ecmRanking(IndexSearcher searcher,
                                   String queryId,
                                   PrintWriter runFile,
                                   ScoreDoc[] scoreDoc,
                                   PrintStream debugStream,
                                   int numResults,
                                   String queryModel) throws IOException {
        Map<String, Float> entityFreq = new HashMap<>();

        if(runFile!=null){
            List<Map.Entry<String, Float>> expansionEntities = marginalizeFreq(
                    numResults, scoreDoc, docInt -> getStrings(searcher, docInt), entityFreq
            );

            HashSet<String> alreadyReturned = new HashSet<>();
            int rank = 1;
            for (Map.Entry<String, Float> expansionEntity : expansionEntities) {

                final String entityId = expansionEntity.getKey();
                final Float score = expansionEntity.getValue();

                if(!alreadyReturned.contains(entityId)) {
                    debugStream.println(entityId + " (" + rank + "):  SCORE " + score);
                    runFile.println(queryId + " Q0 " + entityId + " " + rank + " " + score + " Lucene-ECM-" + queryModel );
                }

                alreadyReturned.add(entityId);

                rank ++;
            }

        }
    }

    @NotNull
    private static Iterable<String> getStrings(@NotNull IndexSearcher searcher, Integer docInt) throws IOException {
        final Document doc = searcher.doc(docInt); // to access stored content
        final IndexableField outLinks = doc.getField(TrecCarRepr.TrecCarSearchField.OutlinkIds.name());
        final IndexableField inLinks = doc.getField(TrecCarRepr.TrecCarSearchField.InlinkIds.name());

        ArrayList<String> result = new ArrayList<>();
        if(outLinks!=null ) {
            final String[] outLinkIds = outLinks.stringValue().split("\n");
            result.addAll(Arrays.asList(outLinkIds));
        } else if(inLinks != null) {
            final String[] inLinkIds = inLinks.stringValue().split("\n");
            result.addAll(Arrays.asList(inLinkIds));
        }

        result.removeIf(String::isEmpty);
        return result;
    }

    private static void ecmPsgRanking(IndexSearcher searcher,
                                      String queryId,
                                      MyQueryBuilder queryBuilder,
                                      String queryStr,
                                      PrintWriter runFile,
                                      ScoreDoc[] scoreDoc,
                                      PrintStream debugStream,
                                      int takeKTerms,
                                      int numResults,
                                      String queryModel,
                                      boolean omitQueryTerms ) throws IOException {
        Map<String, Float> entityFreq = new HashMap<>();

        if(runFile!=null){
            List<Map.Entry<String, Float>> expansionEntities = marginalizeFreq(
                    takeKTerms, scoreDoc, docInt -> getStrings(searcher, docInt), entityFreq
            );


            final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;

            final BooleanQuery booleanQuery = queryBuilder.toEntityRmQuery(queryStr, expansionEntities, omitQueryTerms);

            TopDocs tops = searcher.search(booleanQuery, numResults);
            ScoreDoc[] scoreDoc2 = tops.scoreDocs;


            outputQueryResults(searcher, queryId, runFile, trecCarRepr, scoreDoc2, debugStream, queryModel);

        }
    }

    private static void outputQueryResults(IndexSearcher searcher,
                                           String queryId,
                                           PrintWriter runFile,
                                           TrecCarRepr trecCarRepr,
                                           @NotNull ScoreDoc[] scoreDoc,
                                           PrintStream debugStream,
                                           String queryModel) throws IOException {

        HashSet<String> alreadyReturned = new HashSet<>();
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
                debugStream.println("  " + doc.getField(trecCarRepr.getEntityField().name()).stringValue());
                runFile.println(queryId + " Q0 " + docId + " " + searchRank + " " + searchScore + " Lucene-"+ queryModel );
            }
            alreadyReturned.add(docId);
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