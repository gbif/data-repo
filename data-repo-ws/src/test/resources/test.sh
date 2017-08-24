#!/usr/bin/env bash
curl -i -X POST -H "Content-Type: multipart/form-data" -F "file=@testrepo/10.5072-dp.bvmv02/occurrence.txt" -F "metadata=@testrepo/10.5072-dp.bvmv02/metadata.xml" --user user:password http://api.gbif-dev.org/v1/data_packages

