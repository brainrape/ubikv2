#!/bin/bash

pushd $( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

pushd ..
export VERSION=`boot print-version`
boot build
popd

rm -f *.jar
cp ../target/ubikv2-$VERSION.jar .
docker build -t lesbroot/ubikv2:$VERSION --build-arg jar_version=$VERSION .

popd
