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
 * A search index representation of a Wikipedia entity.
 * @author Laura Dietz
 */
public class TrecCarEntity implements TrecCarPageRepr {

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

  public String idPage(@NotNull Data.Page p) {
        return p.getPageId();
    }


    private static void paragraphContent(@NotNull Data.Para paragraph, @NotNull StringBuilder content) {
        content.append(paragraph.getParagraph().getTextOnly()).append('\n');
    }


    private static void leadContent(@NotNull Data.Page page, @NotNull StringBuilder content) {
        content.append(page.getPageName()).append('\n');

        for (Data.PageSkeleton skeleton : page.getSkeleton()) {
            if (skeleton instanceof Data.Para) {
                paragraphContent((Data.Para) skeleton, content);
            }
        }
    }


    private static void outLinkIds(@NotNull List<Data.PageSkeleton> skeletons, ArrayList<String> content) {
        for (Data.PageSkeleton skeleton : skeletons) {
            if (skeleton instanceof Data.Para) {
                paragraphOutLinkIds(((Data.Para) skeleton).getParagraph(), content);
            } else if (skeleton instanceof Data.Section) {
                outLinkIds(((Data.Section) skeleton).getChildren(), content);
            }
        }
    }


    private static void paragraphOutLinkIds(@NotNull Data.Paragraph p, ArrayList<String> content) {
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
        result.put(TrecCarSearchField.WikiDataQId, p.getPageMetadata().getWikiDataQid());



        final ArrayList<String> outLinks = new ArrayList<>();
        outLinkIds(p.getSkeleton(), outLinks);
        result.put(TrecCarSearchField.OutlinkIds, outLinks);

        // Todo finish
        return Collections.singletonMap(idPage(p), result);
    }

    @NotNull
    private List<String> freqListToStrings(@NotNull ArrayList<Data.ItemWithFrequency<String>> inLinkAnchors) {
        final ArrayList<String> result = new ArrayList<>();
        for (Data.ItemWithFrequency<String> linkAnchor : inLinkAnchors) {
            for (int i = 0; i < linkAnchor.getFrequency(); i++) {
                result.add(linkAnchor.getItem());
            }
        }
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
