package edu.unh.cs;

import edu.unh.cs.treccar_v2.Data;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A search index representation of a trec car paragraph
 */
public class TrecCarEntity implements TrecCarPageRepr {

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

    public String idPage(Data.Page p) {
        return p.getPageId();
    }


    private static void paragraphContent(Data.Para paragraph, StringBuilder content) {
        content.append(paragraph.getParagraph().getTextOnly()).append('\n');
    }


    private static void leadContent(Data.Page page, StringBuilder content) {
        content.append(page.getPageName()).append('\n');

        for (Data.PageSkeleton skel : page.getSkeleton()) {
            if (skel instanceof Data.Para) paragraphContent((Data.Para) skel, content);
            else {
            }    // ignore other
        }
    }


    private static void outLinkIds(List<Data.PageSkeleton> skels, ArrayList<String> content) {
        for (Data.PageSkeleton skel : skels) {
            if (skel instanceof Data.Para) paragraphOutlinkIds(((Data.Para) skel).getParagraph(), content);
            if (skel instanceof Data.Section) outLinkIds(((Data.Section) skel).getChildren(), content);
            else {
            }    // ignore other
        }

    }


    private static void paragraphOutlinkIds(Data.Paragraph p, ArrayList<String> content) {
        for (Data.ParaBody body : p.getBodies()) {
            if (body instanceof Data.ParaLink) {
                Data.ParaLink link = (Data.ParaLink) body;
                content.add(link.getPageId());
            }
        }
    }


    @Override
    @NotNull
    public Map<String, HashMap<TrecCarSearchField, List<String>>> convertPage(Data.Page p) {
        final HashMap<TrecCarSearchField, List<String>> result = new HashMap<>();
        final StringBuilder content = new StringBuilder();

        leadContent(p, content);

        List<String> fullText = new ArrayList<>();
        fullText.add(p.getPageName());
        fullText.add(content.toString());
        fullText.addAll(freqListToStrings(p.getPageMetadata().getInlinkAnchors()));
        fullText.addAll(p.getPageMetadata().getDisambiguationNames());
        result.put(TrecCarSearchField.Text, fullText);

        result.put(TrecCarSearchField.LeadText, Collections.singletonList(content.toString()));
        result.put(TrecCarSearchField.Title, Collections.singletonList(p.getPageName()));
        result.put(TrecCarSearchField.AnchorNames, freqListToStrings(p.getPageMetadata().getInlinkAnchors()));
        result.put(TrecCarSearchField.DisambiguationNames, p.getPageMetadata().getDisambiguationNames());
        result.put(TrecCarSearchField.CategoryNames, p.getPageMetadata().getCategoryNames());
        result.put(TrecCarSearchField.InlinkIds, p.getPageMetadata().getInlinkIds());



        final ArrayList<String> outlinks = new ArrayList<>();
        outLinkIds(p.getSkeleton(), outlinks);
        result.put(TrecCarSearchField.OutlinkIds, outlinks);

        // Todo finish
        return Collections.singletonMap(idPage(p), result);
    }

    private List<String> freqListToStrings(ArrayList<Data.ItemWithFrequency<String>> inlinkAnchors) {
        final ArrayList<String> result = new ArrayList<>();
        for (Data.ItemWithFrequency<String> linkAnchor : inlinkAnchors) {
            for (int i = 0; i < linkAnchor.getFrequency(); i++) {
                result.add(linkAnchor.getItem());
            }
        }
        return result;
    }


    private List<String> getOutlinkIds(Data.Page p) {
        final ArrayList<String> result = new ArrayList<>();

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

        for (TrecCarSearchField field : repr.keySet()) {
            if (field == TrecCarSearchField.OutlinkIds || field == TrecCarSearchField.InlinkIds) {
                // todo not sure how to add multiple of these without being butchered by the standard analyser
                // StringField would take it as a single token, but that's also not really what we want here.
                doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
            } else {
                doc.add(new TextField(field.name(), String.join("\n", repr.get(field)), Field.Store.YES));
            }
        }
        return doc;
    }
}
