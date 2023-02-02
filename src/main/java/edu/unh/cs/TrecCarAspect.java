package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A search index representation of a top-level Wikipedia section ("aspect").
 * @author Laura Dietz
 */
public class TrecCarAspect implements TrecCarPageRepr {

    @Override
    public TrecCarSearchField getIdField() {
        return TrecCarSearchField.Id;
    }

    @Override
    public TrecCarSearchField getWikiDataQIdField() {
        return TrecCarSearchField.WikiDataQId;
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

  @Override
  public Analyzer getAnalyzer(String analyzerStr) {
    return TrecCarRepr.defaultAnalyzer(analyzerStr);
  }

  public String idPage(@NotNull Data.Page p){

        return p.getPageId();
    }

    public String idSection(@NotNull Data.Section s, Data.Page p){
        return idPage(p)+'/'+s.getHeadingId();
    }

    private void sectionHeadings(@NotNull List<Data.PageSkeleton> children, StringBuilder content){
        for(Data.PageSkeleton skeleton: children){
            if (skeleton instanceof Data.Section) {
                sectionHeadings(((Data.Section) skeleton).getChildren(), content);
            }
        }

    }

    private void sectionContent(@NotNull Data.Section section, @NotNull StringBuilder content){
        content.append(section.getHeading()).append('\n');
        for (Data.PageSkeleton skeleton: section.getChildren()) {
            if (skeleton instanceof Data.Section) {
                sectionContent((Data.Section) skeleton, content);
            } else if (skeleton instanceof Data.Para) {
                paragraphContent(((Data.Para) skeleton).getParagraph(), content);
            } else if (skeleton instanceof Data.ListItem) {
                paragraphContent(((Data.ListItem) skeleton).getBodyParagraph(), content);
            }
        }
    }
    private void leadContent(@NotNull Data.Page page, StringBuilder content){
        for (Data.PageSkeleton skeleton: page.getSkeleton()) {
            if (skeleton instanceof Data.Para) {
                paragraphContent(((Data.Para) skeleton).getParagraph(), content);
            } else if (skeleton instanceof Data.ListItem) {
                paragraphContent(((Data.ListItem) skeleton).getBodyParagraph(), content);
            }
        }
    }

    private void paragraphContent(@NotNull Data.Paragraph paragraph, @NotNull StringBuilder content){
        content.append(paragraph.getTextOnly()).append('\n');
    }

    @NotNull
    private List<String> getEntityIdsOnly(@NotNull Data.Paragraph p) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: p.getBodies()){
            if(body instanceof Data.ParaLink){
                result.add(((Data.ParaLink) body).getPageId());
            }
        }
        return result;
    }


    private void sectionEntities(@NotNull Data.Section section, ArrayList<String> entities){
        for (Data.PageSkeleton skeleton: section.getChildren()) {
            if (skeleton instanceof Data.Section) {
                sectionEntities((Data.Section) skeleton, entities);
            } else if (skeleton instanceof Data.Para) {
                paragraphEntities((Data.Para) skeleton, entities);
            }
        }
    }
    private void paragraphEntities(@NotNull Data.Para paragraph, @NotNull ArrayList<String> entities){
        entities.addAll(getEntityIdsOnly(paragraph.getParagraph()));
    }

    @NotNull
    public Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(@NotNull Data.Page p){
        final HashMap<String, HashMap<TrecCarSearchField, List<String>>> result = new HashMap<>();

        for (Data.Section s: p.getChildSections()) {
            result.put(idSection(s,p),convertTopSection(s, p));
        }

        return result;
    }

    public HashMap<TrecCarSearchField, List<String>> convertTopSection(Data.Section s, @NotNull Data.Page p){
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        final StringBuilder content = new StringBuilder();
        final StringBuilder lead = new StringBuilder(p.getPageName());
        sectionContent(s, content);
        leadContent(p, lead);
        result.put(TrecCarSearchField.Text, Collections.singletonList(content +"\n\n\n"+ lead));
        result.put(TrecCarSearchField.LeadText, Collections.singletonList(lead.toString()));
        result.put(TrecCarSearchField.WikiDataQId, p.getPageMetadata().getWikiDataQid());

        final ArrayList<String>  entities = new ArrayList<>();
        sectionEntities(s, entities);
        result.put(TrecCarSearchField.OutlinkIds, entities);

        final StringBuilder headings = new StringBuilder(s.getHeading());
        sectionHeadings(s.getChildren(), headings);
        result.put(TrecCarSearchField.Headings, Collections.singletonList(headings.toString()));
        result.put(TrecCarSearchField.Title, Collections.singletonList(p.getPageName()));


        return result;
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

    @NotNull
    private Document singlePageToLuceneDoc(String id, @NotNull HashMap<TrecCarSearchField, List<String>> repr) {
        final Document doc = new Document();
        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!

        for(TrecCarSearchField field:repr.keySet()) {
            if(field == getWikiDataQIdField()){
                doc.add(new StringField(getWikiDataQIdField().name(), id, Field.Store.YES));  // don't tokenize this!
            } else {
                doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
            }
        }
        return doc;
    }

}
