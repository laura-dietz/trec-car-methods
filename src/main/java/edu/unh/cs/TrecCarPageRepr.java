package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: dietz
 * Date: 1/5/18
 * Time: 3:22 PM
 */
public interface TrecCarPageRepr extends TrecCarRepr {
    String idPage(Data.Page p);

    @NotNull
    Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(Data.Page p);

    @NotNull
    List<Document> pageToLuceneDoc(Data.Page paragraph);

    @Override
    default TrecCarSearchField[] getSearchFields() {
        return TrecCarSearchField.values();
    }
}
