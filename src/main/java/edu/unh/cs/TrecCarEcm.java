package edu.unh.cs;

import edu.unh.cs.data.EntityContextModel;
import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * User: dietz
 * Date: 1/17/18
 * Time: 5:53 PM
 */
public class TrecCarEcm implements TrecCarPageRepr {

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
        return new TrecCarSearchField[]{TrecCarSearchField.Text
                , TrecCarSearchField.EntityLinks
                , TrecCarSearchField.OutlinkIds
                , TrecCarSearchField.Title
                , TrecCarSearchField.Headings
                , TrecCarSearchField.LeadText
        };
    }

    @Override
    public Analyzer getAnalyzer(String analyzerStr) { return TrecCarRepr.defaultAnalyzer(analyzerStr); }

    public String idEcm(@NotNull EntityContextModel.Ecm ecm) {
        return ecm.getEcmId();
    }

    @NotNull
    public Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(Data.Page p) {
        final HashMap<String, HashMap<TrecCarSearchField, List<String>>> result = new HashMap<>();

        for (EntityContextModel.Ecm ecm : EntityContextModel.convertPage(p)) {
            result.put(idEcm(ecm), convertEcm(ecm));
        }
        return result;
    }


    public HashMap<TrecCarSearchField, List<String>> convertEcm(@NotNull EntityContextModel.Ecm ecm){
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        result.put(TrecCarSearchField.Text, Collections.singletonList(ecm.getText()));
        result.put(TrecCarSearchField.OutlinkIds, ecm.getAllEntityIds());
        result.put(TrecCarSearchField.EntityLinks, ecm.getAllEntities());
        result.put(TrecCarSearchField.Title, Collections.singletonList(ecm.getPageTitle()));
        result.put(TrecCarSearchField.Headings, ecm.getHeadings());
        result.put(TrecCarSearchField.LeadText, ecm.getLeadText());
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
