#!/bin/sh

rm result.csv
rm result.txt

java -jar target/microbenchmarks.jar -f 5 -wi 7 -i 7 -rf scsv -rff result.csv -prof hs_rt,gc | tee result.txt

