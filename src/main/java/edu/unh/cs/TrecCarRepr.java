package edu.unh.cs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.HashMap;
import java.util.Map;

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
    TrecCarSearchField getEntityField();
    TrecCarSearchField[] getSearchFields();

    Analyzer getAnalyzer(String analyzerStr);

    static Analyzer defaultAnalyzer(final String analyzerStr) {

        final Analyzer textAnalyzer = ("std".equals(analyzerStr))? new StandardAnalyzer():
                ("english".equals(analyzerStr)? new EnglishAnalyzer(): new StandardAnalyzer());


        final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.OutlinkIds.name(), new WhitespaceAnalyzer());
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.InlinkIds.name(), new WhitespaceAnalyzer());
        fieldAnalyzers.put(TrecCarRepr.TrecCarSearchField.Id.name(), new WhitespaceAnalyzer());
        final DelegatingAnalyzerWrapper queryAnalyzer = new PerFieldAnalyzerWrapper(textAnalyzer, fieldAnalyzers);
        return queryAnalyzer;
    }
}
