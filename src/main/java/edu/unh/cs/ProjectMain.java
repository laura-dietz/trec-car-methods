package edu.unh.cs;

import edu.unh.cs.lucene.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProjectMain {
    public static void main(@NotNull String[] args) {
        System.setProperty("file.encoding", "UTF-8");

        if(args.length < 1){
            System.out.println("Command line must start with mode: (index|query)");
            System.exit(-1);
        }

        if("--tool-version".equals(args[0])) {
            System.out.println("21");
            System.exit(0);
        }

        String mode = args[0];

        if (mode.equals("index")) {
            if(args.length <2){
                TrecCarLuceneIndexer.usage();
            }

            if (args.length < 4) {
                TrecCarLuceneIndexer.usage();
            }

            final String representation = args[1];
            final String cborFile = args[2];
            final String indexPath = args[3];
            final String analyzer = (args.length > 4) ? args[4] : "std";

            new TrecCarLuceneIndexer(representation, cborFile, indexPath, analyzer);
        } else if (mode.equals("query")) {

            if(args.length <2){
                TrecCarLuceneQuery.usage();
            }

            if (args.length < 10) {
                TrecCarLuceneQuery.usage();
            }

            String representation = args[1];
            String queryType = args[2];
            String output = args[3];
            String queryCborFile = args[4];
            String indexPath = args[5];
            String runFileName = args[6];
            String queryModel = args[7];
            String retrievalModel = args[8];
            String expansionModel = args[9];
            String analyzerStr = args[10];
            int numResults = Integer.parseInt(args[11]);
            int numRmExpansionDocs = (args.length > 12)? Integer.parseInt(args[12]): 20;
            int numRmExpansionTerms = (args.length > 13)? Integer.parseInt(args[13]): 20;
            String killQueryEntityString = args[14];
            String rankAggregator = args[15];

            List<String> searchFields = new ArrayList<>();
            if (args.length  > 16) {
                searchFields = Arrays.asList(Arrays.copyOfRange(args, 16, args.length));
            }

            new TrecCarLuceneQuery(representation,queryType,output,queryCborFile,indexPath,runFileName,queryModel,
                    retrievalModel,expansionModel,analyzerStr,numResults,numRmExpansionDocs,
                    numRmExpansionTerms,killQueryEntityString,rankAggregator,searchFields);

        } else {
            System.err.println("ERROR! Mode must be either `index` or `query`.");
        }
    }
}
