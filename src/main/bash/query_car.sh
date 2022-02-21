#!/bin/bash

numResults=1000
numRmExpansionDocs=20
numRmExpansionTerms=20
analyzer="english"
vmopts="-Xmx50g"

# Author: Shubham Chatterjee
# Date: 8/13/2020

usage() {
	echo
	echo "A simple bash script to query a Lucene index of CAR corpus."
	echo "Usage: ./query_car.sh [-m | --mode MODE] [-o | --outline OUTLINE CBOR] [-s | --save SAVE DIR] [-i | --index INDEX DIR] [-j | --jar JAR FILE]"
	echo "NOTE: There are other variables that you can set in the script. We recommend to keep them same for reproducibility."
	echo "    -m | --mode            MODE                       \"page\" for page-level runs and \"section\" for section-level runs."
	echo "    -o | --outline         OUTLINE CBOR FILE           Path to the outline cbor file."
	echo "    -s | --save            SAVE DIR                    Path to directory where run files would be saved."
	echo "    -i | --index           INDEX DIR                   Path to the index directory."
	echo "    -j | --jar             JAR FILE                    Path to the Java \"trec-car-methods.jar\" file."
}


if [ "$#" -eq 0 ]; then
   	usage
	exit 1
fi
# Get the command line parameters

while [ "$1" != "" ];
do
	    case $1 in
		-m | --mode )           	shift
		                        	isSection=$1
		                        	;;

		-o | --outline )          shift
						                  outlineCbor=$1
		                        	;;

		-s | --save )             shift
						                  runDir=$1
		                        	;;

		-i | --index )            shift
					                    indexDir=$1
		                          ;;


		-j | --jar )              shift
    					                jarFile=$1
    		                      ;;


		-h | --help )             usage
		                          exit
		                          ;;


	    esac
	    shift
done



for indexType in "paragraph" "page" "entity"; do
     echo " Querying ${indexType} index in ${indexDir}"

     if [[ "${isSection}" == "page" ]]; then
        queryMode="page"
	      for queryType in "all" "title"; do
	        for retrievalModel in "bm25" "ql"; do
			      for expansionModel in "none" "rm" "ecm" "ecm-psg"; do
 				      rankType="paragraph"
				      if [[ "${indexType}" == "entity" ]]; then rankType="entity"; fi
				      if [[ "${indexType}" == "page" ]]; then rankType="entity"; fi
				      if [[ "${expansionModel}" == "ecm" ]]; then rankType="entity"; fi
				      if [[ "${expansionModel}" == "ecm-rm" ]]; then rankType="entity"; fi

				      cfg="$queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numRmExpansionTerms Text"
				      run="lucene-${indexType}--${rankType}-${queryMode}--${queryType}-${retrievalModel}-${expansionModel}--Text-${analyzer}-k${numResults}.run"
				      java $vmopts -jar "$jarFile" query $indexType $queryMode run "$outlineCbor" "$indexDir" "$runDir"/$run "$cfg"
				      echo "${run}"
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
              if [[ "${indexType}" == "entity" ]]; then rankType="entity"; fi
              if [[ "${indexType}" == "page" ]]; then rankType="entity"; fi
              if [[ "${expansionModel}" == "ecm" ]]; then rankType="entity"; fi
              if [[ "${expansionModel}" == "ecm-rm" ]]; then rankType="entity"; fi

				      cfg="$queryType $retrievalModel $expansionModel $analyzer $numResults $numRmExpansionDocs $numRmExpansionTerms Text"
				      run="lucene-${indexType}--${rankType}-${queryMode}--${queryType}-${retrievalModel}-${expansionModel}--Text-${analyzer}-k${numResults}.run"

				      java $vmopts -jar "$jarFile" query $indexType $queryMode run "$outlineCbor" "$indexDir" "$runDir"/$run "$cfg"
				      echo "${run}"
			    done
		    done
	    done
    fi
done