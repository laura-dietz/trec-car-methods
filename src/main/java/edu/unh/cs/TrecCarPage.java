package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A search index representation of a trec car paragraph
 */
public class TrecCarPage implements TrecCarPageRepr {

    @Override
    public TrecCarSearchField getIdField() {
        return TrecCarSearchField.Id;
    }

    @Override
    public TrecCarSearchField getTextField() {
        return TrecCarSearchField.Text;
    }

    @Override
    public TrecCarSearchField getEntityField() {
        return TrecCarSearchField.OutlinkIds;
    }

    @Override
    public TrecCarSearchField[] getSearchFields() {
        return TrecCarSearchField.values();
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

    private static List<String> getEntityIdsOnly(Data.Paragraph p) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: p.getBodies()){
            if(body instanceof Data.ParaLink){
                result.add(((Data.ParaLink) body).getPageId());
            }
        }
        return result;
    }


    private static void sectionEntities(Data.Section section, ArrayList<String> entities){
        for (Data.PageSkeleton skel: section.getChildren()) {
            if (skel instanceof Data.Section) sectionEntities((Data.Section) skel, entities);
            else if (skel instanceof Data.Para) paragraphEntities((Data.Para) skel, entities);
            else {
            }
        }
        }
    private static void paragraphEntities(Data.Para paragraph, ArrayList<String> entities){
        entities.addAll(getEntityIdsOnly(paragraph.getParagraph()));
    }



    private static void pageContent(Data.Page page, StringBuilder content){
        content.append(page.getPageName()).append('\n');

        for(Data.PageSkeleton skel: page.getSkeleton()){
            if(skel instanceof Data.Section) sectionContent((Data.Section) skel, content);
            else if(skel instanceof Data.Para) paragraphContent((Data.Para) skel, content);
            else {}    // ignore other
        }

    }


    private static void pageEntities(Data.Page page, ArrayList<String> entities){
        for(Data.PageSkeleton skel: page.getSkeleton()){
            if(skel instanceof Data.Section) sectionEntities((Data.Section) skel, entities);
            else if(skel instanceof Data.Para) paragraphEntities((Data.Para) skel, entities);
            else {}    // ignore other
        }

    }

    @NotNull
    public Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(Data.Page p){
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        final StringBuilder content = new StringBuilder();
        pageContent(p, content);
        result.put(TrecCarSearchField.Text, Collections.singletonList(content.toString()));

        final ArrayList<String>  entities = new ArrayList<>();
        pageEntities(p, entities);
        result.put(TrecCarSearchField.OutlinkIds, entities);

        result.put(TrecCarSearchField.InlinkIds, p.getPageMetadata().getInlinkIds());

        final StringBuilder headings = new StringBuilder();
        pageHeadings(p.getSkeleton(), headings);
        result.put(TrecCarSearchField.Headings, Collections.singletonList(headings.toString()));
        result.put(TrecCarSearchField.Title, Collections.singletonList(p.getPageName()));

        // Todo finish
        return  Collections.singletonMap(idPage(p), result);
    }

    @Override
    @NotNull
    public List<Document> pageToLuceneDoc(Data.Page page) {
        final Map<String, HashMap<TrecCarSearchField, List<String>>> reprs = convertPage(page);
        final List<Document> docs = new ArrayList<>();

        for (String id : reprs.keySet()) {
            final Document doc = singlePageToLuceneDoc(id, reprs.get(id));
            docs.add(doc);
        }
        return docs;
    }

//    @NotNull
//    private Document singlePageToLuceneDoc(String id, HashMap<TrecCarSearchField, List<String>> repr) {
//        final Document doc = new Document();
//        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!
//
//        for(TrecCarSearchField field:repr.keySet()) {
//            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
//        }
//        return doc;
//    }


    @NotNull
    private Document singlePageToLuceneDoc(String id, HashMap<TrecCarSearchField, List<String>> repr) {
        final Document doc = new Document();
        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!

        for (TrecCarSearchField field : repr.keySet()) {
            if (field == TrecCarSearchField.OutlinkIds || field == TrecCarSearchField.InlinkIds) {
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
