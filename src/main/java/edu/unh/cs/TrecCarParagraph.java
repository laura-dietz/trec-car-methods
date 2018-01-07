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
public class TrecCarParagraph implements TrecCarRepr {

    @Override
    public TrecCarSearchField getIdField() {
        return TrecCarSearchField.Id;
    }

    @Override
    public TrecCarSearchField getTextField() {
        return TrecCarSearchField.Text;
    }

    @Override
    public TrecCarSearchField[] getSearchFields() {
        return TrecCarSearchField.values();
    }


    public String idParagraph(Data.Paragraph p){
        return p.getParaId();
    }


    @NotNull
    public HashMap<TrecCarSearchField, List<String>> convertParagraph(Data.Paragraph p){
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        result.put(TrecCarSearchField.Text, Collections.singletonList(p.getTextOnly()));
        result.put(TrecCarSearchField.EntityLinks, TrecCarReprUtils.getEntitiesOnly(p));
        return result;
    }

    @NotNull
    public Document paragraphToLuceneDoc(Data.Paragraph paragraph) {
        final HashMap<TrecCarSearchField, List<String>> repr = convertParagraph(paragraph);
        String id = idParagraph(paragraph);
        final Document doc = new Document();
        doc.add(new StringField(getIdField().name(), id, Field.Store.YES));  // don't tokenize this!

        for(TrecCarSearchField field:repr.keySet()) {
            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
        }
        return doc;
    }


}
