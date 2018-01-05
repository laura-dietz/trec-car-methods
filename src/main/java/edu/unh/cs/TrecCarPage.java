package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A search index representation of a trec car paragraph
 */
public class TrecCarPage {

    public static enum PageField {
        Id(0), Text(1), Headings(2), Title(3);

        private int value;
        private PageField(int value) {
            this.value = value;
        }

        private static PageField[] values = null;
        public static PageField fromInt(int i) {
            if (PageField.values == null) {
                PageField.values = PageField.values();
            }
            return PageField.values[i];
        }
    }

    public String idPage(Data.Page p){
        return p.getPageId();
    }

    private static void pageHeadings(List<Data.PageSkeleton> children, StringBuilder content){
        for(Data.PageSkeleton skel: children){
            if(skel instanceof Data.Section) pageHeadings(((Data.Section) skel).getChildren(), content);
            else {}    // ignore other
        }

    }



    private static void sectionContent(Data.Section section, StringBuilder content){
        content.append(section.getHeading()+'\n');
        for (Data.PageSkeleton skel: section.getChildren()) {
            if (skel instanceof Data.Section) sectionContent((Data.Section) skel, content);
            else if (skel instanceof Data.Para) paragraphContent((Data.Para) skel, content);
            else {
            }
        }
        }
    private static void paragraphContent(Data.Para paragraph, StringBuilder content){
        content.append(paragraph.getParagraph().getTextOnly()).append('\n');
    }



    private static void pageContent(Data.Page page, StringBuilder content){
        content.append(page.getPageName()).append('\n');

        for(Data.PageSkeleton skel: page.getSkeleton()){
            if(skel instanceof Data.Section) sectionContent((Data.Section) skel, content);
            else if(skel instanceof Data.Para) paragraphContent((Data.Para) skel, content);
            else {}    // ignore other
        }

    }

    @NotNull
    public HashMap<PageField, List<String>> convertPage(Data.Page p){
        final HashMap<PageField, List<String>> result = new HashMap<>();
        final StringBuilder content = new StringBuilder();
        pageContent(p, content);
        result.put(PageField.Text, Collections.singletonList(content.toString()));

        final StringBuilder headings = new StringBuilder();
        pageHeadings(p.getSkeleton(), headings);
        result.put(PageField.Headings, Collections.singletonList(headings.toString()));
        result.put(PageField.Title, Collections.singletonList(p.getPageName()));

        // Todo finish
        return result;
    }

    @NotNull
    public Document pageToLuceneDoc(Data.Page paragraph) {
        final HashMap<PageField, List<String>> repr = convertPage(paragraph);
        String id = idPage(paragraph);
        final Document doc = new Document();
        doc.add(new StringField("ID", id, Field.Store.YES));  // don't tokenize this!

        for(PageField field:repr.keySet()) {
            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
        }
        return doc;
    }



}
