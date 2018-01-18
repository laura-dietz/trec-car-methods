package edu.unh.cs;

/**
 * User: dietz
 * Date: 1/6/18
 * Time: 6:23 PM
 */
public interface TrecCarRepr {

    enum TrecCarSearchField {
        Id(0), Text(1), Headings(2), Title(3), AnchorNames(4), DisambiguationNames(5), CategoryNames(6)
        , InlinkIds(7), OutlinkIds(8), EntityLinks(9), Entity(10), LeadText(11);

        private int value;
        private TrecCarSearchField(int value) {
            this.value = value;
        }

        private static TrecCarSearchField[] values = null;
        public static TrecCarSearchField fromInt(int i) {
            if (TrecCarSearchField.values == null) {
                TrecCarSearchField.values = TrecCarSearchField.values();
            }
            return TrecCarSearchField.values[i];
        }
    }

    TrecCarSearchField getIdField();
    TrecCarSearchField getTextField();
    TrecCarSearchField[] getSearchFields();


}
