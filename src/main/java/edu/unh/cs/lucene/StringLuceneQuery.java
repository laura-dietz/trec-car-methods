package edu.unh.cs.lucene;


import edu.unh.cs.lucene.TrecCarLuceneQuery.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

import java.io.*;
import java.util.*;

/**
 * User: dietz
 * Date: 8/29/18
 * Time: 9:33 AM
 */
public class StringLuceneQuery {

  public static void main(String[] args) throws IOException {
    System.setProperty("file.encoding", "UTF-8");
    boolean includeInlinkEntities = (System.getProperty("car.include-inlink-entities") != null);

    if (args.length < 1) {
      TrecCarLuceneQuery.usage();
    }
    if ("--tool-version".equals(args[0])) {
      System.out.println("9");
      System.exit(0);
    }

    if (args.length < 9)
      TrecCarLuceneQuery.usage();

    final String representation = args[0];
    TrecCarLuceneConfig.LuceneIndexConfig icfg = TrecCarLuceneConfig.getLuceneIndexConfig(representation);

    String queryType = args[1];
    String output = args[2];
    TrecCarLuceneConfig.LuceneQueryConfig cfg = new TrecCarLuceneConfig.LuceneQueryConfig(icfg, !("display".equals(output)), "section".equals(queryType));


    final String queryFile = args[3];
    final String indexPath = args[4];
    final String runFileName = args[5];

    String queryModel = args[6];
    String retrievalModel = args[7];
    String expansionModel = args[8];
    String analyzerStr = args[9];
    int numResults = Integer.parseInt(args[10]);

    if (args.length > 11) {
      try {
        Integer.parseInt(args[11]);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Trying to parse numRmExpansionDocs out of argument 11: \"" + args[11] + "\"");
      }
    }
    int numRmExpansionDocs = (args.length > 11) ? Integer.parseInt(args[11]) : 20;
    int numEcmExpansionDocs = (args.length > 12) ? Integer.parseInt(args[12]) : 100;
    int numRmExpansionTerms = (args.length > 13) ? Integer.parseInt(args[13]) : 20;

    List<String> searchFields = null;
    if (args.length > 13) searchFields = Arrays.asList(Arrays.copyOfRange(args, 13, args.length));


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


    System.out.println("Index loaded from " + indexPath + "/" + cfg.getIndexConfig().getIndexName());
    IndexSearcher searcher = TrecCarLuceneQuery.setupIndexSearcher(indexPath, cfg.getIndexConfig().indexName);

    if ("bm25".equals(retrievalModel)) searcher.setSimilarity(new BM25Similarity());
    else if ("ql".equals(retrievalModel)) searcher.setSimilarity(new LMDirichletSimilarity(1500));
    // else default similarity

    List<String> searchFieldsUsed;
    if (searchFields == null) searchFieldsUsed = cfg.getIndexConfig().getSearchFields();
    else searchFieldsUsed = searchFields;


//        final Analyzer queryAnalyzer = ("std".equals(analyzerStr))? new StandardAnalyzer():
//                ("english".equals(analyzerStr)? new EnglishAnalyzer(): new StandardAnalyzer());
    final Analyzer queryAnalyzer = icfg.trecCarRepr.getAnalyzer(analyzerStr);

    final MyQueryBuilder queryBuilder = new MyQueryBuilder(queryAnalyzer, searchFieldsUsed, icfg.trecCarRepr);
//    final QueryBuilder.QueryStringBuilder queryStringBuilder = ...;

    final PrintWriter runfile = new PrintWriter(runFileName);

    final BufferedReader fileInputStream3 = new BufferedReader(new FileReader(new File(queryFile)));
    HashSet<String> alreadyQueried = new HashSet<>();
    for (String line= fileInputStream3.readLine(); line !=null ; line = fileInputStream3.readLine()) {
      if(!line.trim().isEmpty()) {
        final String[] splits = line.split("\t");
        final String queryId = splits[0];
        final String queryStr = splits[1];


        if (splits.length != 2)
          throw new RuntimeException("Query line \"" + line + "\" does not have exactly two fields. Expected format queryId \\t queryText");

        if (!cfg.outputAsRun) System.out.println("\n\nQueryId: " + queryId);
        if (!cfg.outputAsRun) System.out.println("\n\nQueryStr: " + queryStr);

        final ArrayList<String> queryEntities = new ArrayList<>();
        if (!alreadyQueried.contains(queryId)) {
          TrecCarLuceneQuery.expandedRetrievalModels(cfg, searcher, queryBuilder, runfile, queryStr, queryId, queryEntities, expansionModel, numResults, numRmExpansionDocs, numEcmExpansionDocs, numRmExpansionTerms, queryModel);
          alreadyQueried.add(queryId);
        }
      }
    }
    System.out.println();


    System.out.println("Written to " + runFileName);
    runfile.close();
  }

}
