package edu.unh.cs.data;

import java.util.*;

import edu.unh.cs.treccar_v2.Data;


/**
 * User: dietz
 * Date: 1/17/18
 * Time: 5:58 PM
 */
public class EntityContextModel {
    public static class Ecm {
        public String paragraphId;
        public String text;
        private final List<String> allEntityIds;
        public String pageTitle;
        public List<String> headings;
        public List<String> allEntities;
        private final List<String> leadText;

        public Ecm(String paragraphId, String text, List<String> allEntities, List<String> allEntityIds, String pageTitle, List<String> headings, List<String> leadText) {
            this.paragraphId = paragraphId;
            this.text = text;
            this.allEntityIds = allEntityIds;
            this.pageTitle = pageTitle;
            this.headings = headings;
            this.allEntities = allEntities;
            this.leadText = leadText;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public List<String> getHeadings() {
            return headings;
        }

        public String getEcmId() {
            return getParagraphId();
        }


        public String getParagraphId() {
            return paragraphId;
        }

        public String getText() {
            return text;
        }

        public List<String> getAllEntities() {
            return allEntities;
        }

        public List<String> getAllEntityIds() {
            return allEntityIds;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ecm)) return false;
            Ecm ecm = (Ecm) o;
            return Objects.equals(getParagraphId(), ecm.getParagraphId()) &&
                    Objects.equals(getPageTitle(), ecm.getPageTitle());
        }

        @Override
        public int hashCode() {

            return Objects.hash(getParagraphId(), getPageTitle());

        }

        @Override
        public String toString() {
            return "Ecm{" +
                    "paragraphId='" + paragraphId + '\'' +
                    ", text='" + text + '\'' +
                    ", allEntityIds=" + allEntityIds +
                    ", pageTitle='" + pageTitle + '\'' +
                    ", headings=" + headings +
                    ", allEntities=" + allEntities +
                    ", leadText=" + leadText +
                    '}';
        }

        public List<String> getLeadText() {
            return leadText;
        }
    }

    public static List<Ecm> convertPage (Data.Page page){
        final ArrayList<Ecm> result = new ArrayList<>();
        final List<String> leadText = leadContent(page);

        final List<Data.Page.SectionPathParagraphs> sectionPathParagraphs = page.flatSectionPathsParagraphs();
        for (Data.Page.SectionPathParagraphs sectionPathParagraph : sectionPathParagraphs) {
            final List<Data.Section> sectionPath = sectionPathParagraph.getSectionPath();
            final List<String> headings = new ArrayList<>();
            for(Data.Section section:sectionPath){
                headings.add(section.getHeading());
            }
            final Data.Paragraph paragraph = sectionPathParagraph.getParagraph();

            final String text = paragraph.getTextOnly();
            final List<String> allEntities = paragraph.getEntitiesOnly();
            final List<String> allEntityIds = getEntityIdsOnly(paragraph);
            final String paraId = paragraph.getParaId();
            final String pageTitle = page.getPageName();

            final Ecm ecm = new Ecm(paraId, text, allEntities, allEntityIds, pageTitle, headings, leadText);
            result.add(ecm);
        }

        return result;
    }

    public static List<String> getEntityIdsOnly(Data.Paragraph paragraph) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: paragraph.getBodies()){
            if(body instanceof Data.ParaLink){
                final String pageId = ((Data.ParaLink) body).getPageId();
                assert(!pageId.isEmpty()):"Link with empty page id in paragraph: "+paragraph;
                result.add(pageId);
            }
        }
        return result;
    }

    public static List<String> leadContent(Data.Page page) {
        final ArrayList<String> result = new ArrayList<>();
        for (Data.PageSkeleton skel : page.getSkeleton()) {
            if (skel instanceof Data.Para) {
                Data.Para para = (Data.Para) skel;
                final String text = para.getParagraph().getTextOnly();
                if(text.isEmpty()) System.err.println("Warning: empty paragraph text on page "+page.getPageId()+", paragraph "+para);
                result.add(text);
            }
            else {
            }    // ignore other
        }
        return result;
    }



}
