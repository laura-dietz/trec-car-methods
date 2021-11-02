#!/bin/bash


analyzer="english"
outlineCbor="/home/sc1242/work/bert_entity_ranking/dbpedia_car/all/data/queries-v2_stopped.cbor"
runDir="/home/sc1242/work/bert_entity_ranking/dbpedia_car/all/features/entity-features"
indexDir="/mnt/grapes/share/lucene-v21-14"
queryMode="page"
queryType="title"
numResults=1000
numRmExpansionDocs=20
numEcmExpansionDocs=100
numRmExpansionTerms=20
vmopts="-Xmx50g"
jarFile="/home/sc1242/work/trec-car-methods-0.15-jar-with-dependencies.jar"



for indexType in "paragraph" "page" "entity" "aspect"; do
  echo " Querying $indexType index in $indexDir"
	for retrievalModel in "bm25" "ql"; do
			for expansionModel in "none" "rm" "ecm" "ecm-psg"; do
 				rankType="paragraph"
				if [[ "${indexType}" == "aspect" ]]; then rankType="aspect"; fi
				if [[ "${indexType}" == "entity" ]]; then rankType="entity"; fi
				if [[ "${indexType}" == "page" ]]; then rankType="entity"; fi
				if [[ "${expansionModel}" == "ecm" ]]; then rankType="entity"; fi
				if [[ "${expansionModel}" == "ecm-rm" ]]; then rankType="entity"; fi
				cfg="$queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numEcmExpansionDocs $numRmExpansionTerms Text Headings Title EntityLinks Entity LeadText"
				run="lucene-luceneIndex$indexDir-lucene-$indexType--$rankType-$queryMode--$queryType-$retrievalModel-$expansionModel--Text-$analyzer-$numResults-dbpedia_v2_car.cbor.run"
				java $vmopts -jar $jarFile query $indexType $queryMode run $outlineCbor $indexDir $runDir/$run "$cfg"
				echo "${run}"
			done
	done
done


