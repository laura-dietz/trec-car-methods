package edu.unh.cs.lucene;

import edu.unh.cs.TrecCarRepr;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
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
 * Example of how to build a lucene index of trec car paragraphs
 */
public class TrecCarLuceneQuery {

    private final static boolean produceEcmEntityRanking = true;

    private final static PrintStream SYSTEM_NULL = new PrintStream(new OutputStream() {
        public void write(int b) {
            //DO NOTHING
        }
    });
    private final static PrintWriter SYSTEM_NULL_WRITER = new PrintWriter(SYSTEM_NULL);

    public static class MyQueryBuilder {
        TrecCarRepr trecCarParaRepr;
        String paragraphIndexName = "paragraph.lucene";

        private final Analyzer analyzer;
        private final List<String> searchFields;
        private final TrecCarRepr trecCarRepr;
        private List<String> tokens;
        private List<String> entityTokens;
        private final String textSearchField;
        private final String entitySearchField;

        public MyQueryBuilder(Analyzer analyzer, List<String> searchFields, TrecCarRepr trecCarRepr){
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

        /** TOkenizes the query and stores results in `this.tokens`. -- Not thread safe!*/
        private void tokenizeQuery(String queryStr, String textSearchField, List<String> tokens) throws IOException {
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

        public BooleanQuery toEntityRmQuery(String queryStr, List<Map.Entry<String, Float>> entityRelevanceModel) throws IOException {


            tokenizeQuery(queryStr, textSearchField, tokens);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            for (String searchField : this.searchFields) {
                for (String token : tokens) {
                    booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)),1.0f), BooleanClause.Occur.SHOULD);
                }
            }

            // add Entity RM terms
            for (String entitySearchField : Collections.singletonList(this.entitySearchField)) {
                for (Map.Entry<String, Float> stringFloatEntry : entityRelevanceModel.subList(0, Math.min(entityRelevanceModel.size(), (64-tokens.size())))) {
                  List<String> entityToks = new ArrayList<>();
                  tokenizeQuery(stringFloatEntry.getKey(), entitySearchField, entityToks);
                  for(String entity: entityToks) {
                      float weight = stringFloatEntry.getValue();
                        booleanQuery.add(new BoostQuery(new TermQuery(new Term(entitySearchField, entity)),weight), BooleanClause.Occur.SHOULD);
//                      booleanQuery.add(new BoostQuery(new TermQuery(new Term(entitySearchField, entity)), 1.0f), BooleanClause.Occur.SHOULD);
                    }
                }
            }

            return booleanQuery.build();
        }

        public BooleanQuery toWhitelistRmQuery(String queryStr, List<String> whitelist) throws IOException {


            tokenizeQuery(queryStr, textSearchField, tokens);
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

            for (String searchField : this.searchFields) {
                for (String token : tokens) {
                    booleanQuery.add(new BoostQuery(new TermQuery(new Term(searchField, token)),1.0f), BooleanClause.Occur.SHOULD);
                }
            }

            // add Entity RM terms
            for (String entitySearchField : Collections.singletonList(TrecCarRepr.TrecCarSearchField.Id.toString())) {
                for (String docId: whitelist) {
                  List<String> docTocs = new ArrayList<>();
                  tokenizeQuery(docId, entitySearchField, docTocs);
                  for(String docIdTerm: docTocs) {
                        booleanQuery.add(new BoostQuery(new TermQuery(new Term(entitySearchField, docIdTerm)),100), BooleanClause.Occur.SHOULD);
//                      booleanQuery.add(new BoostQuery(new TermQuery(new Term(entitySearchField, entity)), 1.0f), BooleanClause.Occur.SHOULD);
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

    private static void usage() {
        System.out.println("Command line parameters: (paragraph|page|entity|ecm|aspect) " +
                " (section|page) (run|display) OutlineCBOR INDEX RUNFile" +
                " (sectionPath|all|subtree|title|leafheading|interior)" +
                " (bm25|ql|default) (none|rm|ecm|ecm-rm|ecm-psg) (std|english) numResults " +
                "numRmExpansionDocs numEcmExpansionDocs numRmExpansionTerms [searchField1] [searchField2] ...\n" +
                "searchFields one of "+Arrays.toString(TrecCarRepr.TrecCarSearchField.values()));
        System.exit(-1);
    }
//        System.out.println("Command line parameters: (paragraphs|pages)CBOR LuceneINDEX");


    private static String queryModel;
    private static String retrievalModel;
    private static String expansionModel;
    private static String analyzerStr;
    private static int numResults;
    private static int numEcmExpansionDocs=100;
    private static int numRmExpansionDocs=20;
    private static int numRmExpansionTerms=20;

    public static void main(String[] args) throws IOException {
        System.setProperty("file.encoding", "UTF-8");
        boolean includeInlinkEntities = (System.getProperty("car.include-inlink-entities")!= null);

        if(args.length <1){
            usage();
        }
        if("--tool-version".equals(args[0])) {
            System.out.println("9");
            System.exit(0);
        }

        if (args.length < 9)
            usage();

        final String representation = args[0];
        TrecCarLuceneConfig.LuceneIndexConfig icfg = TrecCarLuceneConfig.getLuceneIndexConfig(representation);

        String queryType = args[1];
        String output = args[2];
        TrecCarLuceneConfig.LuceneQueryConfig cfg = new TrecCarLuceneConfig.LuceneQueryConfig(icfg, !("display".equals(output)), "section".equals(queryType));


        final String queryCborFile = args[3];
        final String indexPath = args[4];
        final String runFileName = args[5];

        queryModel = args[6];
        retrievalModel = args[7];
        expansionModel = args[8];
        analyzerStr = args[9];
        numResults = Integer.parseInt(args[10]);

        if(args.length > 11){
            try {
                Integer.parseInt(args[11]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Trying to parse numRmExpansionDocs out of argument 11: \""+args[11]+"\"");
            }
        }
        numRmExpansionDocs = (args.length > 11)? Integer.parseInt(args[11]): 20;
        numEcmExpansionDocs = (args.length > 12)? Integer.parseInt(args[12]): 100;
        numRmExpansionTerms = (args.length > 13)? Integer.parseInt(args[13]): 20;

        List<String> searchFields = null;
        if (args.length  > 13) searchFields = Arrays.asList(Arrays.copyOfRange(args, 13, args.length));


        System.out.println("queryType = " + queryType);
        System.out.println("representation = " + representation);
        System.out.println("queryModel = " + queryModel);
        System.out.println("retrievalModel = " + retrievalModel);
        System.out.println("expansionModel = " + expansionModel);
        System.out.println("analyzerStr = " + analyzerStr);
        System.out.println("numResults = " + numResults);
        System.out.println("numRmExpansionDocs = " + numRmExpansionDocs);
        System.out.println("numRmExpansionTerms = " + numRmExpansionTerms);
        System.out.println("numEcmExpansionDocs = " + numEcmExpansionDocs);


        System.out.println("Index loaded from "+indexPath+"/"+cfg.getIndexConfig().getIndexName());
        IndexSearcher searcher = setupIndexSearcher(indexPath, cfg.getIndexConfig().indexName);

        if ("bm25".equals(retrievalModel)) searcher.setSimilarity(new BM25Similarity());
        else if ("ql".equals(retrievalModel)) searcher.setSimilarity(new LMDirichletSimilarity(1500));
        // else default similarity

        List<String> searchFieldsUsed;
        if (searchFields == null) searchFieldsUsed = cfg.getIndexConfig().getSearchFields();
        else searchFieldsUsed = searchFields;


//        final Analyzer queryAnalyzer = ("std".equals(analyzerStr))? new StandardAnalyzer():
//                ("english".equals(analyzerStr)? new EnglishAnalyzer(): new StandardAnalyzer());
        final Analyzer queryAnalyzer = icfg.trecCarRepr.getAnalyzer(analyzerStr);

        final MyQueryBuilder queryBuilder = new MyQueryBuilder(queryAnalyzer, searchFieldsUsed, icfg.trecCarRepr );
        final QueryBuilder.QueryStringBuilder queryStringBuilder =
                ("sectionpath".equals(queryModel))? new QueryBuilder.SectionPathQueryStringBuilder() :
                        ("all".equals(queryModel) ? new QueryBuilder.OutlineQueryStringBuilder():
                            ("subtree".equals(queryModel) ? new QueryBuilder.SubtreeQueryStringBuilder():
                               ("title".equals(queryModel) ? new QueryBuilder.TitleQueryStringBuilder():
                                   ("leafheading".equals(queryModel) ? new QueryBuilder.LeafHeadingQueryStringBuilder():
                                       ("interior".equals(queryModel) ? new QueryBuilder.InteriorHeadingQueryStringBuilder():
                                         new QueryBuilder.SectionPathQueryStringBuilder()
                                   )))));



        final PrintWriter runfile = new PrintWriter(runFileName);

        if(cfg.queryAsSection ) {
            final FileInputStream fileInputStream3 = new FileInputStream(new File(queryCborFile));
            for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream3)) {
                HashSet<String> alreadyQueried = new HashSet<>();
                System.out.println("\n\nPage: " + page.getPageId());
                final ArrayList<String> queryEntities =(includeInlinkEntities) ? page.getPageMetadata().getInlinkIds(): new ArrayList<>();
                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                    System.out.println();
                    System.out.println(Data.sectionPathId(page.getPageId(), sectionPath) + "   \t " + Data.sectionPathHeadings(sectionPath));

                    final String queryStr = queryStringBuilder.buildSectionQueryStr(page, sectionPath);
                    final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);
                    if(!alreadyQueried.contains(queryId)) {
                        expandedRetrievalModels(cfg, searcher, queryBuilder, runfile, queryStr, queryId, queryEntities);
                        alreadyQueried.add(queryId);
                    }
                }
            }
            System.out.println();
        }
        else { //if(!cfg.queryAsSection){
            final FileInputStream fileInputStream3 = new FileInputStream(new File(queryCborFile));
            HashSet<String> alreadyQueried = new HashSet<>();
            for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream3)) {
                if (!cfg.outputAsRun)  System.out.println("\n\nPage: "+page.getPageId());

                final String queryStr = queryStringBuilder.buildSectionQueryStr(page, Collections.emptyList());
                final String queryId = page.getPageId();
                final ArrayList<String> queryEntities =(includeInlinkEntities) ? page.getPageMetadata().getInlinkIds(): new ArrayList<>();
                if(!alreadyQueried.contains(queryId)) {
                    expandedRetrievalModels(cfg, searcher, queryBuilder, runfile, queryStr, queryId, queryEntities);
                    alreadyQueried.add(queryId);
                }
            }
            System.out.println();
        }

        System.out.println("Written to "+runFileName);
        runfile.close();

    }

    private static void expandedRetrievalModels(TrecCarLuceneConfig.LuceneQueryConfig cfg, IndexSearcher searcher, MyQueryBuilder queryBuilder, PrintWriter runfile, String queryStr, String queryId, List<String> queryEntities) throws IOException {
        PrintStream debugStream = (!cfg.isOutputAsRun()?System.out:SYSTEM_NULL);
        if ("ecm-psg".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, queryEntities); // change back
            ecmPsgRanking(searcher, queryId, queryBuilder, queryStr, runfile, scoreDocs, debugStream, numRmExpansionTerms);
        } else if ("ecm-rm".equals(expansionModel)){
            final ScoreDoc[] scoreDocs = oneExpandedQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, queryBuilder.trecCarRepr.getTextField().name());
            ecmRanking(searcher, queryId, runfile, scoreDocs, debugStream);
        } else if ("ecm".equals(expansionModel)) {
            final ScoreDoc[] scoreDocs = oneQuery(searcher, queryBuilder, queryStr, queryId, SYSTEM_NULL_WRITER, debugStream, queryEntities); // change back
            ecmRanking(searcher, queryId, runfile, scoreDocs, debugStream);
        } else if ("rm".equals(expansionModel)){
            oneExpandedQuery(searcher, queryBuilder, queryStr, queryId, runfile, debugStream, queryBuilder.trecCarRepr.getTextField().name());
        } else if ("none".equals(expansionModel)){
            oneQuery(searcher, queryBuilder, queryStr, queryId, runfile, debugStream, queryEntities);
        } else {
            System.out.println("Warning: expansion model "+expansionModel+" not known.");
            oneQuery(searcher, queryBuilder, queryStr, queryId, runfile, debugStream, queryEntities);
        }
    }


    private static List<Map.Entry<String, Float>> relevanceModel(IndexSearcher searcher, MyQueryBuilder queryBuilder, String queryStr, int takeKDocs, int takeKTerms, PrintStream debugStream, String expansionField) throws IOException {
        final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;
        final BooleanQuery booleanQuery = queryBuilder.toQuery(queryStr);
        TopDocs tops = searcher.search(booleanQuery, takeKDocs);
        ScoreDoc[] scoreDoc = tops.scoreDocs;

        final Map<String, Float> wordFreqs = new HashMap<>();
        queryBuilder.addTokens(queryStr, 1.0f, wordFreqs);

        // guess if we have log scores...
        boolean useLog = false;
        for (ScoreDoc score : scoreDoc) {
            if (score.score < 0.0) useLog = true;
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
            Double weight = useLog ? (score.score - normalizer) : (score.score / normalizer);
            String docContents = searcher.doc(score.doc).get(expansionField);
            queryBuilder.addTokens(docContents, weight.floatValue(), wordFreqs);
        }

        ArrayList<Map.Entry<String, Float>> allWordFreqs = new ArrayList<>(wordFreqs.entrySet());
        allWordFreqs.sort(new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> kv1, Map.Entry<String, Float> kv2) {
                return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
            }
        });

        List<Map.Entry<String, Float>> expansionTerms = allWordFreqs.subList(0, Math.min(takeKTerms, allWordFreqs.size()));

        System.out.println("RM3 Expansions: "+expansionTerms.toString());
        return expansionTerms;
    }


    public static interface FetchEntries{
        public Iterable<String> entries(Integer docInt) throws IOException;
    }
    private static List<Map.Entry<String, Float>> marginalizeFreqs(int takeKDocs, int takeKTerms, ScoreDoc[] scoreDoc, FetchEntries fetchEntries, Map<String, Float> wordFreqs ) throws IOException {
        // guess if we have log scores...
        boolean useLog = false;
        for (ScoreDoc score : scoreDoc) {
            if (score.score < 0.0) useLog = true;
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
            Double weight = useLog ? (score.score - normalizer) : (score.score / normalizer);
            for ( String entry: fetchEntries.entries(score.doc) ) {
                wordFreqs.compute(entry, (t, oldV) ->
                        (oldV==null)? weight.floatValue(): (oldV + weight.floatValue())
                );
            }
        }

        ArrayList<Map.Entry<String, Float>> allWordFreqs = new ArrayList<>(wordFreqs.entrySet());
        allWordFreqs.sort(new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> kv1, Map.Entry<String, Float> kv2) {
                return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
            }
        });

        List<Map.Entry<String, Float>> expansionTerms = allWordFreqs.subList(0, Math.min(takeKTerms, allWordFreqs.size()));

        System.out.println("other Expansions: "+expansionTerms.toString());
        return expansionTerms;
    }



    private static ScoreDoc[] oneExpandedQuery(IndexSearcher searcher, MyQueryBuilder queryBuilder, String queryStr, String queryId, PrintWriter runfile, PrintStream debugStream, String expansionField) throws IOException {
        final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;

        final List<Map.Entry<String, Float>> relevanceModel = relevanceModel(searcher, queryBuilder, queryStr, numRmExpansionDocs, numRmExpansionTerms, debugStream, expansionField);
        final BooleanQuery booleanQuery = queryBuilder.toRm3Query(queryStr, relevanceModel);

        TopDocs tops = searcher.search(booleanQuery, numResults);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        System.out.println("Found "+scoreDoc.length+" RM3 results.");

        outputQueryResults(searcher, queryId, runfile, trecCarRepr, scoreDoc, debugStream);
        return scoreDoc;
    }

    private static ScoreDoc[] oneQuery(IndexSearcher searcher, MyQueryBuilder queryBuilder, String queryStr, String queryId, PrintWriter runfile, PrintStream debugStream, List<String> queryEntities) throws IOException {
        final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;
//        List<Map.Entry<String, Float>> queryEntities2 = new ArrayList<>();
//        for (String entity: queryEntities){
//            queryEntities2.add(new AbstractMap.SimpleEntry<String, Float>(entity,1f));
//        }
        final BooleanQuery booleanQuery = queryBuilder.toWhitelistRmQuery(queryStr, queryEntities);
        TopDocs tops = searcher.search(booleanQuery, numResults);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        System.out.println("Found "+scoreDoc.length+" results.");

        outputQueryResults(searcher, queryId, runfile, trecCarRepr, scoreDoc, debugStream);

        return tops.scoreDocs;
    }

    private static void ecmRanking(IndexSearcher searcher, String queryId, PrintWriter runfile, ScoreDoc[] scoreDoc, PrintStream debugStream) throws IOException {
        Map<String, Float> entityFreqs = new HashMap<>();

        if(runfile!=null){
            List<Map.Entry<String, Float>> expansionEntities = marginalizeFreqs(numEcmExpansionDocs, numResults, scoreDoc, new FetchEntries() {
                @Override
                public Iterable<String> entries(Integer docInt) throws IOException {
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
            }, entityFreqs);

            HashSet<String> alreadyReturned = new HashSet<String>();
            int rank = 1;
            for (Map.Entry<String, Float> expansionEntity : expansionEntities) {

                final String entityId = expansionEntity.getKey();
                final Float score = expansionEntity.getValue();

                if(!alreadyReturned.contains(entityId)) {
                    debugStream.println(entityId + " (" + rank + "):  SCORE " + score);
                    runfile.println(queryId + " Q0 " + entityId + " " + rank + " " + score + " Lucene-ECM-" + queryModel + "-" + retrievalModel);
                }

                alreadyReturned.add(entityId);

                rank ++;
            }

        }
    }

    private static void ecmPsgRanking(IndexSearcher searcher, String queryId, MyQueryBuilder queryBuilder, String queryStr, PrintWriter runfile, ScoreDoc[] scoreDoc, PrintStream debugStream, int takeKTerms) throws IOException {
        Map<String, Float> entityFreqs = new HashMap<>();

        if(runfile!=null){
            List<Map.Entry<String, Float>> expansionEntities = marginalizeFreqs(numEcmExpansionDocs, takeKTerms, scoreDoc, new FetchEntries() {
                @Override
                public Iterable<String> entries(Integer docInt) throws IOException {
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
            }, entityFreqs);


            final TrecCarRepr trecCarRepr = queryBuilder.trecCarRepr;

            final BooleanQuery booleanQuery = queryBuilder.toEntityRmQuery(queryStr, expansionEntities);

            TopDocs tops = searcher.search(booleanQuery, numResults);
            ScoreDoc[] scoreDoc2 = tops.scoreDocs;
            System.out.println("Found "+scoreDoc2.length+" Entity-RM results.");

            outputQueryResults(searcher, queryId, runfile, trecCarRepr, scoreDoc2, debugStream);

        }
    }

    private static void outputQueryResults(IndexSearcher searcher, String queryId, PrintWriter runfile, TrecCarRepr trecCarRepr, ScoreDoc[] scoreDoc, PrintStream debugStream) throws IOException {
        HashSet<String> alreadyReturned = new HashSet<String>();
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
                runfile.println(queryId + " Q0 " + docId + " " + searchRank + " " + searchScore + " Lucene-"+queryModel+"-"+retrievalModel);
            }
            alreadyReturned.add(docId);
        }
    }

    @NotNull
    private static IndexSearcher setupIndexSearcher(String indexPath, String typeIndex) throws IOException {
        Path path = FileSystems.getDefault().getPath(indexPath, typeIndex);
        Directory indexDir = FSDirectory.open(path);
        IndexReader reader = DirectoryReader.open(indexDir);
        return new IndexSearcher(reader);
    }


}