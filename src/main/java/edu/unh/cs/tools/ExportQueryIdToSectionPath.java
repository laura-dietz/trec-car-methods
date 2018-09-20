package edu.unh.cs.tools;

import edu.unh.cs.lucene.QueryBuilder;
import edu.unh.cs.lucene.TrecCarLuceneQuery;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * User: dietz
 * Date: 9/20/18
 * Time: 3:09 PM
 */
public class ExportQueryIdToSectionPath {

  public static void main(String[] args) throws IOException {
    System.setProperty("file.encoding", "UTF-8");

    if(args.length < 2) throw new RuntimeException("Usage: queryCborFile outputTsvFile");
    String queryCborFile = args[0];
    String outputTsv = args[1];

    final BufferedWriter outputFile = new BufferedWriter(new FileWriter(new File(outputTsv)));

//    final TrecCarLuceneQuery.MyQueryBuilder queryBuilder = new TrecCarLuceneQuery.MyQueryBuilder(queryAnalyzer, searchFieldsUsed, icfg.trecCarRepr );
    final QueryBuilder.QueryStringBuilder queryStringBuilder = new QueryBuilder.SectionPathQueryStringBuilder();

      final FileInputStream fileInputStream3 = new FileInputStream(new File(queryCborFile));
      for (Data.Page page : DeserializeData.iterableAnnotations(fileInputStream3)) {
        HashSet<String> alreadyQueried = new HashSet<>();
        System.out.println("\n\nPage: " + page.getPageId());

        for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
          System.out.println();
          System.out.println(Data.sectionPathId(page.getPageId(), sectionPath) + "   \t " + Data.sectionPathHeadings(sectionPath));

          final String queryStr = queryStringBuilder.buildSectionQueryStr(page, sectionPath);
          final String queryId = Data.sectionPathId(page.getPageId(), sectionPath);

          outputFile.write(queryId + '\t' + queryStr);
        }
      }

      System.out.println();

      outputFile.close();
    }

}
