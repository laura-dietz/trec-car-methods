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
public class TrecCarParagraph {

    public static enum ParagraphField {
        Id(0), Text(1), Links(2);

        private int value;
        private ParagraphField(int value) {
            this.value = value;
        }

        private static ParagraphField[] values = null;
        public static ParagraphField fromInt(int i) {
            if (ParagraphField.values == null) {
                ParagraphField.values = ParagraphField.values();
            }
            return ParagraphField.values[i];
        }
    }

    public String idParagraph(Data.Paragraph p){
        return p.getParaId();
    }


    @NotNull
    public HashMap<ParagraphField, List<String>> convertParagraph(Data.Paragraph p){
        final HashMap<ParagraphField, List<String>> result = new HashMap<>();
        result.put(ParagraphField.Text, Collections.singletonList(p.getTextOnly()));
//        result.put(ParagraphField.Links, TrecCarReprUtils.getEntitiesOnly(p));
        return result;
    }

    @NotNull
    public Document paragraphToLuceneDoc(Data.Paragraph paragraph) {
        final HashMap<ParagraphField, List<String>> repr = convertParagraph(paragraph);
        String id = idParagraph(paragraph);
        final Document doc = new Document();
        doc.add(new StringField("ID", id, Field.Store.YES));  // don't tokenize this!

        for(ParagraphField field:repr.keySet()) {
            doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
        }
        return doc;
    }


}
