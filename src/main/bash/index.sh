#!/bin/bash

paragraphCbor="/home/sc1242/work/trec_car/paragraphCorpus/dedup.articles-paragraphs.cbor"
allButBenchmarkCbor="/home/sc1242/work/trec_car/unprocessedAllButBenchmark.v2.1/unprocessedAllButBenchmark.Y2.cbor"
saveDir="/home/sc1242/work/trec_car/index"
analyzer="english"
jarFile="/home/sc1242/work/trec-car-methods-0.15-jar-with-dependencies.jar"

for representation in "paragraph" "entity" "page" "aspect"; do
  echo "Representation: $representation"
  cborFile=$allButBenchmarkCbor
  if [[ "${representation}" == "paragraph" ]]; then cborFile=$paragraphCbor; fi
  java -jar $jarFile index $representation $cborFile $saveDir $analyzer
  echo "========================================================================="
done
