package edu.unh.cs.lucene;

import edu.unh.cs.treccar_v2.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * User: dietz
 * Date: 1/17/18
 * Time: 5:18 PM
 */
public class QueryBuilder {

    static public interface QueryStringBuilder {
        @NotNull
        String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath);
    }

    static public class SectionPathQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();

        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();
                queryStr.append(page.getPageName());
                for (Data.Section section : sectionPath) {
                    queryStr.append(" ").append(section.getHeading());
                }
                return queryStr.toString();
            }
        }
    }
    static public class OutlineQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            StringBuilder queryStr = new StringBuilder();
            Queue<Data.PageSkeleton> queue = new ArrayDeque<>();

            queryStr.append(page.getPageName());
            queue.addAll(page.getSkeleton());

            while(!queue.isEmpty()){
                Data.PageSkeleton skel = queue.poll();
                if( skel instanceof Data.Section ) {
                    Data.Section section = (Data.Section) skel;
                    queryStr.append(" ").append(section.getHeading());
                    queue.addAll(((Data.Section) skel).getChildren());
                }
            }

            return queryStr.toString();
        }

    }
    static public class SubtreeQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new OutlineQueryStringBuilder();
        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to OutlineQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();
                Queue<Data.PageSkeleton> queue = new ArrayDeque<>();

                Data.Section leafSection = sectionPath.get(sectionPath.size() - 1);

                queryStr.append(leafSection.getHeading());
                queue.addAll(leafSection.getChildren());

                while (!queue.isEmpty()) {
                    Data.PageSkeleton skel = queue.poll();
                    if (skel instanceof Data.Section) {
                        Data.Section section = (Data.Section) skel;
                        queryStr.append(" ").append(section.getHeading());
                        queue.addAll(((Data.Section) skel).getChildren());
                    }
                }

                return queryStr.toString();
            }
        }
    }
    static public class TitleQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            return page.getPageName();
        }
    }
    static public class LeafHeadingQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();

        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {

                Data.Section section = sectionPath.get(sectionPath.size() - 1);
                return section.getHeading();
            }
        }
    }
    static public class InteriorHeadingQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();
        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();
                for (int i = 0; i < sectionPath.size() - 1; i++) { // -1 !
                    Data.Section section = sectionPath.get(i);
                    queryStr.append(" ").append(section.getHeading());
                }
                return queryStr.toString();
            }
        }
    }
    static public class ParagraphQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath) {
            StringBuilder queryStr = new StringBuilder();
            Queue<Data.PageSkeleton> queue = new ArrayDeque<>();
            int paraCount = 0;
            int paraCharThresh = 200;
//            queryStr.append(page.getPageName());
            queue.addAll(page.getSkeleton());

            while(!queue.isEmpty()){
                Data.PageSkeleton skel = queue.poll();
                if( skel instanceof Data.Section ) {
                    queue.addAll(((Data.Section) skel).getChildren());
                }
                else if (skel instanceof Data.Para ){
                    Data.Paragraph para = ((Data.Para) skel).getParagraph();
                    queryStr.append(" ").append(para.getTextOnly());
                    paraCount ++;
                    if (queryStr.length()>=paraCharThresh) {
                        return  queryStr.toString();
                    }

                }
            }

            return queryStr.toString();
        }

    }
}
