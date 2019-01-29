#!/usr/bin/env bash

set -euo pipefail

cd skil-server
echo "RUNNING SKIL BUILD in " `pwd`

echo "BUILDING SKIL APIS"
cd skil-apis
mvn -T 1C -B -e clean install -DskipTests -Pci-nexus
cd ../../

echo "BUILDING ZEPPELIN"
sh change-scala-versions.sh ${SCALA_VERSION}
cd zeppelin/
git checkout skymind-0.7-skil-1.2.0
mvn -B -e clean install -Pbuild-distr -P "${SPARK_VERSION}" -P "${HADOOP_VERSION}" -Pyarn -Ppyspark -Psparkr -DskipTests -P ci-nexus
cd ../

echo "BUILDING SKIL"
cd skil-server/
mvn -T 1C -B -e clean install -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Pci-nexus

echo "BUILDING MODEL SERVER"
cd  modelserver/
mvn -T 1C -B -e clean install -DskipTests -Pnative -Ptf-cpu  -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
cd ../

echo "BUILDING RPMs"
if [ "$PYTHON_PACKAGE_BUILD" == "true" ]; then
    mvn -B -e -Pbuilddistro -Pmodelserver -Pgenerate-tarball -Ppython-rpm -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package
    mvn -B -e -Pbuilddistro -Pmodelserver -Pgenerate-rpm -Prpm -Ppython-rpm -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package
else
    mvn -B -e -Pbuilddistro -Pmodelserver -Pgenerate-tarball -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package
    mvn -B -e -Pbuilddistro -Pmodelserver -Pgenerate-rpm -Prpm -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package
fi
