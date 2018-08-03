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
 * A search index representation of a trec car aspect
 */
public class TrecCarNames implements TrecCarPageRepr {

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
        return TrecCarSearchField.InlinkIds;
    }

    @Override
    public TrecCarSearchField[] getSearchFields() {
        return TrecCarSearchField.values();
    }

  @Override
  public Analyzer getAnalyzer(String analyzerStr) {
    return TrecCarRepr.defaultAnalyzer(analyzerStr);
  }

  public String idPage(Data.Page p){
        return p.getPageId();
    }

    public String idSection(Data.Section s,Data.Page p){
        return idPage(p)+'/'+s.getHeadingId();
    }

    private static void sectionHeadings(List<Data.PageSkeleton> children, StringBuilder content){
        for(Data.PageSkeleton skel: children){
            if(skel instanceof Data.Section) sectionHeadings(((Data.Section) skel).getChildren(), content);
            else {}    // ignore other
        }

    }



    private static void namesContent(Data.Page page, StringBuilder content){
        content.append(page.getPageName());
        content.append('\n');

        int i=0;
        for(Data.ItemWithFrequency<String> anchor:page.getPageMetadata().getInlinkAnchors()){
            if(i>10) break;
            content.append(anchor.getItem());
            content.append(' ');
            i++;
        }

        content.append('\n');

        for(String name: page.getPageMetadata().getDisambiguationNames()) {
            content.append(name);
            content.append(' ');
        }

        content.append('\n');

        for(String name: page.getPageMetadata().getRedirectNames()) {
            content.append(name);
            content.append(' ');
        }

    }



    @NotNull
    public Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(Data.Page p){
        final HashMap<String, HashMap<TrecCarSearchField, List<String>>> result = new HashMap<>();

            result.put(idPage(p), convertMetadata(p));

        return result;
    }

    public HashMap<TrecCarSearchField, List<String>> convertMetadata(Data.Page p){
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        final StringBuilder names = new StringBuilder();
        namesContent(p, names);
        result.put(TrecCarSearchField.Text, Collections.singletonList(names.toString()));
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
    private Document singlePageToLuceneDoc(String id, HashMap<TrecCarSearchField, List<String>> repr) {
        final Document doc = new Document();
        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!

        for(TrecCarSearchField field:repr.keySet()) {
            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
        }
        return doc;
    }



}
