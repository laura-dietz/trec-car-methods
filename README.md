# trec-car-methods
Implementation of various retrieval methods for trec-car 

Main class for command line launching:  edu.unh.cs.ProjectMain


# Create Index


Call Main Class with arguments:

`index (paragraph|page|entity|ecm|aspect|names) CBOR LuceneINDEX (std|english)`

Where:

* "index" is a keyword that activates the indexing mode
* (paragraph|page|entity|ecm|aspect|names) select the index type with
    * "paragraph": creates an index of paragraphs with entity Links (to be used with paragraph files)
    * "page": creates an index of whole Wikipedia pages (all visible text) as well as entity links, anchor text, inlinks
    * "entity": creates an index of the lead text along with typical features used in entity linking like various names from title, anchor text, disambiguation names, and redirects, but also inlinks (as page title and entity id)
    * "ecm": expand pages with information about entities
    * "aspect": index each page section separately, preserving title/heading
    * "names": similar to "entity" just without full text info. All names are also concatenated in the "Text" search field.
* `CBOR` is the data file of the paragraph Corpus or AllButBenchmark
* `LuceneINDEX` is a local directory where the index will be stored (multiple types of indexes can go in the same directory)
* (std|english) select the tokenizer with
    * std: Lucene Standard Analyzer
    * english: Lucene English Analyzer


# Run Queries

This is designed to work with a previousy created index (`INDEX`) and TREC CAR queries which come in the form of page outlines with title, headings, and subheadings

Call Main Class with argument:
`query (paragraph|page|entity|ecm|aspect)  (section|page|pageViaSection) (run|display) OutlineCBOR INDEX RUNFile (sectionPath|all|subtree|title|leafHeading|interior) (bm25|ql|default) (none|rm|ecm|ecm-rm|ecm-psg|rm1|ecm-psg1) (std|english) numResults numRmExpansionDocs (killQueryEntities|none) numRmExpansionTerms [searchField1] [searchField2] ...
searchFields one of [Id,  Text, Headings, Title, AnchorNames, DisambiguationNames, CategoryNames, InlinkIds, OutlinkIds, EntityLinks, Entity, LeadText, WikiDataQId]`




Where:

* "query" is a keyword that activates the query mode
* (paragraph|page|entity|ecm|aspect) selects the index type used (see indexing above)
*  (section|page|pageViaSection) selects the query mode with
    * "section":  create a ranking for every section (i.e., section path) in the query outline
    * "page": create a single ranking for every query outline
    * "pageViaSection": will produce a ranking per section, then merge rankings (via RM3 normalized rank scores). It is recommended to use a section-level query model.
* (run|display) selects the output either
    * "run" create rankings for all queries in batch model, using the TREC EVAL Run file format (depending on the index and retrieval model these are paragraph ids or entity ids )
    * "display" display the contents of retrieved elements (with additional info on the query model)
* `OutlineCBOR`: path to the query outline file in CBOR format
* `INDEX`: path to the search index directory (see indexing above)
* `RUNFile`: path to the which the run file will be written
*  (sectionPath|all|subtree|title|leafHeading|interior) selects how the search query is constructed using one of the following
    * "title": (page mode) search query uses the title of the query outline in clean text (ignoring other headings) -- designed to work in "page" mode, when applied to sections, it will produce the same ranking for all sections.
    * "all": (page mode) search query uses the title and all headings/subheadings/subsub...etc in the outline, with a cutoff after 64 query terms. -- designed to work in "page" mode, when applied to sections it will produce the same ranking for all sections
    * "sectionPath": (section mode) search query comprised of the title, the heading, and all parent headings on the path between section and title 
    * "leafHeading": (section mode) search query only uses the lowest heading of the queried section
    * "interior" (section mode) like sectionPath, but without title and heading of this section (i.e., only parent headings)
    * "subtree" (section mode) search query comprised of the heading of this section and headings of all child sections (i.e., no headings of other sections).
* (bm25|ql|default) selection of the base retrieval model, one of
    * "bm25" the BM25 retrieval model as implemented by Lucene
    * "ql" the query likelihood model with Dirichlt Moothing (using mu=1500)
    * "default" same as "bm25"
*  (none|rm|ecm|ecm-rm|ecm-psg|rm1|ecm-psg1) selection of the query expansion model
    * "none": no expansion, just base retrieval model
    * "rm": relevance model3 expansion with using query terms with weight 1, and remaining terms with a weight according to their relevance model weight
    * "rm1": like rm, but does not include query terms (aka RM1)
    * "ecm": identifies frequently mentioned entities in search results of the base ranking, uses the relevance model to combine frequency and search score. Produces a ranking of entities.
    * "ecm-rm": like ecm, but will use the "rm" to identify mentioned entities
    * "ecm-psg": uses entities in ecm to expand the search query to retrieve a ranking, will include query terms with weight 1 and expansion entities with weight according to the relevance model. Entities will only be searched via entity id (URL encoded wiki title) in the entity field of the index (e.g. outlinks for page-indexes and inlinks for entity-indexes)
    * "ecm-psg1" like ecm-psg, but does not include query terms
* (std|english) selection of the tokenizer -- must be the same as used during index creation!
* `numResults`: number of total entries per ranking
* `numRmExpansionDocs`: number of documents used for query expansion (rm and ecm-psg) 
* `numRmExpansionTerms`: number of expansion terms (for rm and rm1 and ecm-rm) or expansion entities (for ecm-psg and ecm-psg1)
* (killQueryEntities|none): selects one of
    * none: no effect
    * killQueryEntities: will ignore entityIds when they are the same as the queryId during entity expansion, entity ranking, and when producing ranking outputs.  -- this is necessary when using entity ranking with TREC CAR's allButBenchmark corpus, where ground truth can be leaked by matching the query id against all inlinks. Activating this option will avoid the leakage.
* Searchfield ... : a list of indexed fields that will be used during full text search (e.g. "Text"). Entities will automatically be searched in the entity field, not the full text.

