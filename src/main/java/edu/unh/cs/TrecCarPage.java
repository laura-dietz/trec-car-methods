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
 * A search index representation of a Wikipedia page.
 * @author Laura Dietz
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

  @Override
  public Analyzer getAnalyzer(String analyzerStr) {
    return TrecCarRepr.defaultAnalyzer(analyzerStr);
  }

  public String idPage(@NotNull Data.Page p){
        return p.getPageId();
    }

    private void pageHeadings(@NotNull List<Data.PageSkeleton> children, StringBuilder content){
        for(Data.PageSkeleton skeleton: children) {
            if(skeleton instanceof Data.Section) {
                pageHeadings(((Data.Section) skeleton).getChildren(), content);
            }
        }
    }

    private void sectionContent(@NotNull Data.Section section, @NotNull StringBuilder content){
        content.append(section.getHeading()).append('\n');
        for (Data.PageSkeleton skeleton: section.getChildren()) {
            if (skeleton instanceof Data.Section) {
                sectionContent((Data.Section) skeleton, content);
            } else if (skeleton instanceof Data.Para) {
                paragraphContent((Data.Para) skeleton, content);
            }
        }
    }

    private void paragraphContent(@NotNull Data.Para paragraph, @NotNull StringBuilder content){
        content.append(paragraph.getParagraph().getTextOnly()).append('\n');
    }

    private void pageContent(@NotNull Data.Page page, @NotNull StringBuilder content){
        content.append(page.getPageName()).append('\n');

        for(Data.PageSkeleton skeleton: page.getSkeleton()){
            if (skeleton instanceof Data.Section) {
                sectionContent((Data.Section) skeleton, content);
            } else if (skeleton instanceof Data.Para) {
                paragraphContent((Data.Para) skeleton, content);
            }
        }
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
        for (Data.PageSkeleton skeleton : section.getChildren()) {
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

    private void pageEntities(@NotNull Data.Page page, ArrayList<String> entities){
        for(Data.PageSkeleton skeleton : page.getSkeleton()) {
            if (skeleton instanceof Data.Section) {
                sectionEntities((Data.Section) skeleton, entities);
            } else if (skeleton instanceof Data.Para) {
                paragraphEntities((Data.Para) skeleton, entities);
            }

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
        result.put(TrecCarSearchField.WikiDataQId, p.getPageMetadata().getWikiDataQid());

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

    @NotNull
    private Document singlePageToLuceneDoc(String id, @NotNull HashMap<TrecCarSearchField, List<String>> repr) {
        final Document doc = new Document();
        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!

        for (TrecCarSearchField field : repr.keySet()) {
            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
        }
        return doc;
    }


}
