package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

/**
 * User: dietz
 * Date: 1/5/18
 * Time: 3:22 PM
 */
public interface TrecCarPageRepr {
    String idPage(Data.Page p);

    @NotNull
    HashMap<PageField, List<String>> convertPage(Data.Page p);

    @NotNull
    Document pageToLuceneDoc(Data.Page paragraph);

    enum PageField {
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
}
