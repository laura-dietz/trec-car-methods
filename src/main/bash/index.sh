#!/bin/bash

analyzer="english"


# Author: Shubham Chatterjee
# Date: 8/13/2020

usage() {
	echo
	echo "A simple bash script to create a Lucene index of CAR corpus."
	echo "Usage: ./index.sh [--paragraph-cbor PARAGRAPH CBOR] [--knowledge-base ALL BUT BENCHMARK CBOR] [--save SAVE DIR] [--jar JAR FILE]"
	echo "NOTE: There are other variables that you can set in the script. We recommend to keep them same for reproducibility."
	echo "    --paragraph-cbor         PARAGRAPH CBOR FILE                  Path to the paragraphCorpus cbor file."
	echo "    --knowledge-base         ALL BUT BENCHMARK CBOR FILE          Path to the \"allButBenchmark.cbor\" file."
	echo "    --save                   SAVE DIR                             Path to directory where indexes would be saved."
	echo "    --jar                    JAR FILE                             Path to the Java \"trec-car-methods.jar\" file."
}


if [ "$#" -eq 0 ]; then
   	usage
	exit 1
fi
# Get the command line parameters

while [ "$1" != "" ];
do
	    case $1 in

		--paragraph-cbor )        shift
						                  paragraphCbor=$1
		                        	;;

		--knowledge-base )        shift
    						              allButBenchmarkCbor=$1
    		                      ;;

		--save )                  shift
						                  saveDir=$1
		                        	;;

		--jar )                   shift
    					                jarFile=$1
    		                      ;;


		-h | --help )             usage
		                          exit
		                          ;;


	    esac
	    shift
done


for representation in "paragraph" "entity" "page" "aspect"; do
  echo "Representation: $representation"
  cborFile=$allButBenchmarkCbor
  if [[ "${representation}" == "paragraph" ]]; then cborFile=$paragraphCbor; fi
  java -jar "$jarFile" index $representation "$cborFile" "$saveDir" $analyzer
  echo "========================================================================="
done
