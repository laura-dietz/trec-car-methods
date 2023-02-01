package edu.unh.cs.lucene;

import edu.unh.cs.treccar_v2.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Builds a Lucene query.
 * @author Laura Dietz
 */
public class QueryBuilder {

    public interface QueryStringBuilder {
        @NotNull
        String buildSectionQueryStr(Data.Page page, List<Data.Section> sectionPath);
    }

    /**
     * Builds a query from the page name and top-level section headings of the Wikipedia page.
     */
    static public class SectionPathQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();

        @NotNull
        public String buildSectionQueryStr(Data.Page page, @NotNull List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();
                queryStr.append(page.getPageName());
                for (Data.Section section : sectionPath) {
                    queryStr.append(" ").append(section.getHeading());
                }
                return queryStr.toString().replaceAll("\t", "s");
            }
        }
    }

    /**
     * Build a query from the page name and the top-level section headings (including the headings of the child sections).
     */
    static public class OutlineQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(@NotNull Data.Page page, List<Data.Section> sectionPath) {
            StringBuilder queryStr = new StringBuilder();

            queryStr.append(page.getPageName());
            Queue<Data.PageSkeleton> queue = new ArrayDeque<>(page.getSkeleton());

            while(!queue.isEmpty()){
                Data.PageSkeleton skeleton = queue.poll();
                if( skeleton instanceof Data.Section ) {
                    Data.Section section = (Data.Section) skeleton;
                    queryStr.append(" ").append(section.getHeading());
                    queue.addAll(((Data.Section) skeleton).getChildren());
                }
            }

            return queryStr.toString().replaceAll("\t", "s");
        }

    }
    static public class SubtreeQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new OutlineQueryStringBuilder();
        @NotNull
        public String buildSectionQueryStr(Data.Page page, @NotNull List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to OutlineQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();

                Data.Section leafSection = sectionPath.get(sectionPath.size() - 1);

                queryStr.append(leafSection.getHeading());
                Queue<Data.PageSkeleton> queue = new ArrayDeque<>(leafSection.getChildren());

                while (!queue.isEmpty()) {
                    Data.PageSkeleton skeleton = queue.poll();
                    if (skeleton instanceof Data.Section) {
                        Data.Section section = (Data.Section) skeleton;
                        queryStr.append(" ").append(section.getHeading());
                        queue.addAll(((Data.Section) skeleton).getChildren());
                    }
                }

                return queryStr.toString().replaceAll("\t", "s");
            }
        }
    }
    static public class TitleQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(@NotNull Data.Page page, List<Data.Section> sectionPath) {
            return page.getPageName().replaceAll("\t", "s");
        }
    }
    static public class LeafHeadingQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();

        @NotNull
        public String buildSectionQueryStr(Data.Page page, @NotNull List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {

                Data.Section section = sectionPath.get(sectionPath.size() - 1);
                return section.getHeading().replaceAll("\t", "s");
            }
        }
    }
    static public class InteriorHeadingQueryStringBuilder implements QueryStringBuilder {
        final static QueryStringBuilder defaultQueryStringBuilder = new TitleQueryStringBuilder();
        @NotNull
        public String buildSectionQueryStr(Data.Page page, @NotNull List<Data.Section> sectionPath) {
            if(sectionPath.isEmpty()) {
                System.err.println("Warning: sectionPath is empty. This QueryStringBuilder is identical to TitleQueryStringBuilder");
                return defaultQueryStringBuilder.buildSectionQueryStr(page, sectionPath);
            } else {
                StringBuilder queryStr = new StringBuilder();
                for (int i = 0; i < sectionPath.size() - 1; i++) { // -1 !
                    Data.Section section = sectionPath.get(i);
                    queryStr.append(" ").append(section.getHeading());
                }
                return queryStr.toString().replaceAll("\t", "s");
            }
        }
    }
    static public class ParagraphQueryStringBuilder implements QueryStringBuilder {
        @NotNull
        public String buildSectionQueryStr(@NotNull Data.Page page, List<Data.Section> sectionPath) {
            StringBuilder queryStr = new StringBuilder();
            int paraCharThresh = 200;
            Queue<Data.PageSkeleton> queue = new ArrayDeque<>(page.getSkeleton());

            while(!queue.isEmpty()){
                Data.PageSkeleton skel = queue.poll();
                if( skel instanceof Data.Section ) {
                    queue.addAll(((Data.Section) skel).getChildren());
                }
                else if (skel instanceof Data.Para ){
                    Data.Paragraph para = ((Data.Para) skel).getParagraph();
                    queryStr.append(" ").append(para.getTextOnly());
                    if (queryStr.length()>=paraCharThresh) {
                        return  queryStr.toString().replaceAll("\t", "s");
                    }

                }
            }

            return queryStr.toString().replaceAll("\t", "s");
        }

    }
}
