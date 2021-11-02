#!/bin/bash

isSection=$1
analyzer="english"
outlineCbor="/media/shubham/My Passport/Ubuntu/Desktop/research/trec-car/benchmarkY1-train.v2.0/benchmarkY1/benchmarkY1-train/train.pages.cbor-outlines.cbor"
outlineCborName="benchmarkY1train.v201.cbor.outlines"
runDir="/media/shubham/My Passport/Ubuntu/Desktop/research/trec-car/benchmarkY1-train.v2.0/benchmarkY1/benchmarkY1-train"
indexDir="/media/shubham/My Passport/Ubuntu/Desktop/research/trec-car/index"
numResults=1000
numRmExpansionDocs=20
numEcmExpansionDocs=100
numRmExpansionTerms=20
vmopts="-Xmx50g"
jarFile="/home/shubham/Desktop/others/trec-car-methods/target/trec-car-methods-0.15-jar-with-dependencies.jar"


for indexType in "paragraph" "page" "entity" "aspect"; do
     echo " Querying ${indexType} index in ${indexDir}"

     if [[ "${isSection}" == "page" ]]; then
        queryMode="page"
	      for queryType in "all" "title"; do
	        for retrievalModel in "bm25" "ql"; do
			      for expansionModel in "none" "rm" "ecm" "ecm-psg"; do
 				      rankType="paragraph"
				      if [[ "${indexType}" == "aspect" ]]; then rankType="aspect"; fi
				      if [[ "${indexType}" == "entity" ]]; then rankType="entity"; fi
				      if [[ "${indexType}" == "page" ]]; then rankType="entity"; fi
				      if [[ "${expansionModel}" == "ecm" ]]; then rankType="entity"; fi
				      if [[ "${expansionModel}" == "ecm-rm" ]]; then rankType="entity"; fi

				      #cfg=$queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numEcmExpansionDocs $numRmExpansionTerms Text
				      run="lucene-luceneindex$indexType-lucene-$indexType--$rankType-$queryMode--$queryType-$retrievalModel-$expansionModel--Text-$analyzer-k$numResults-$outlineCborName.run"
				      java $vmopts -jar $jarFile query $indexType $rankType run "$outlineCbor" "$indexDir" "$runDir"/"$run" $queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numEcmExpansionDocs $numRmExpansionTerms "Text"
				      echo "$run"
			      done
		      done
	      done
    fi

    if [[ "${isSection}" == "section" ]]; then
	    queryMode="section"
	    for queryType in "sectionPath" "leafHeading" "subtree" "title" "all" "interior"; do
		    for retrievalModel in "bm25" "ql"; do
			    for expansionModel in "none" "rm" "ecm" "ecm-psg"; do
              rankType="paragraph"
			        if [[ "${indexType}" == "aspect" ]]; then rankType="aspect"; fi
              if [[ "${indexType}" == "entity" ]]; then rankType="entity"; fi
              if [[ "${indexType}" == "page" ]]; then rankType="entity"; fi
              if [[ "${expansionModel}" == "ecm" ]]; then rankType="entity"; fi
              if [[ "${expansionModel}" == "ecm-rm" ]]; then rankType="entity"; fi
				      #cfg=$queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numEcmExpansionDocs $numRmExpansionTerms Text
				      run="lucene-luceneindex$indexDir-lucene-$indexType--$rankType-$queryMode--$queryType-$retrievalModel-$expansionModel--Text-$analyzer-k$numResults-$outlineCborName.run"
				      java ${vmopts} -jar $jarFile query ${indexType} ${queryMode} run "$outlineCbor" "$indexDir" "$runDir"/"$run" $queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numEcmExpansionDocs $numRmExpansionTerms "Text"
				      echo "${run}"
			    done
		    done
	    done
    fi
done
