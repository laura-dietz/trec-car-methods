package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A search index representation of a trec car paragraph
 */
public class TrecCarEntity implements TrecCarPageRepr {

    @Override
    public String idPage(Data.Page p){
        return p.getPageId();
    }



    private static void paragraphContent(Data.Para paragraph, StringBuilder content){
        content.append(paragraph.getParagraph().getTextOnly()).append('\n');
    }



    private static void leadContent(Data.Page page, StringBuilder content){
        content.append(page.getPageName()).append('\n');

        for(Data.PageSkeleton skel: page.getSkeleton()){
            if(skel instanceof Data.Para) paragraphContent((Data.Para) skel, content);
            else {}    // ignore other
        }

    }


    private static void outLinkIds(List<Data.PageSkeleton> skels, ArrayList<String> content){
        for(Data.PageSkeleton skel: skels){
            if(skel instanceof Data.Para) paragraphOutlinkIds(((Data.Para) skel).getParagraph(), content);
            if(skel instanceof Data.Section) outLinkIds(((Data.Section) skel).getChildren(), content);
            else {}    // ignore other
        }

    }


    private static void paragraphOutlinkIds(Data.Paragraph p, ArrayList<String> content){
        for(Data.ParaBody body: p.getBodies()){
            if (body instanceof Data.ParaLink) {
                Data.ParaLink link = (Data.ParaLink) body;
                content.add(link.getPageId());
            }
        }
    }


    @Override
    @NotNull
    public HashMap<PageField, List<String>> convertPage(Data.Page p){
        final HashMap<PageField, List<String>> result = new HashMap<>();
        final StringBuilder content = new StringBuilder();
        leadContent(p, content);
        result.put(PageField.Text, Collections.singletonList(content.toString()));
        result.put(PageField.Title, Collections.singletonList(p.getPageName()));
        result.put(PageField.AnchorNames, p.getPageMetadata().getInlinkAnchors());
        result.put(PageField.DisambiguationNames, p.getPageMetadata().getDisambiguationNames());
        result.put(PageField.CategoryNames, p.getPageMetadata().getCategoryNames());
        result.put(PageField.InlinkIds, p.getPageMetadata().getInlinkIds());

        final ArrayList<String> outlinks = new ArrayList<>();
        outLinkIds(p.getSkeleton(), outlinks);
        result.put(PageField.OutlinkIds, outlinks);

        // Todo finish
        return result;
    }

    private List<String> getOutlinkIds(Data.Page p) {
        final ArrayList<String> result = new ArrayList<>();

        return result;
    }

    @Override
    @NotNull
    public Document pageToLuceneDoc(Data.Page paragraph) {
        final HashMap<PageField, List<String>> repr = convertPage(paragraph);
        String id = idPage(paragraph);
        final Document doc = new Document();
        doc.add(new StringField("ID", id, Field.Store.YES));  // don't tokenize this!

        for(PageField field:repr.keySet()) {
            if (field == PageField.OutlinkIds || field == PageField.InlinkIds){
                // todo not sure how to add multiple of these without being butchered by the standard analyser
                // StringField would take it as a single token, but that's also not really what we want here.
                doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
            } else {
                doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
            }
        }
        return doc;
    }



}
