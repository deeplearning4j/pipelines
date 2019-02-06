#!/usr/bin/env bash
set -e

export DEBIAN_FRONTEND=noninteractive

# Cleanup
apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

find ./docker/ubuntu/artifacts -name 'skil-*.deb' -exec apt-get -qqy install  {} \;

su skil -c 'source /etc/skil/skil-env.sh; java -cp "/opt/skil/lib/*:/opt/skil/logback/*" io.skymind.zeppelin.main.InstallInterpreters'

#chown -R skil:skil /opt/skil/miniconda

#su skil -c "mvn -B -e -Pci-nexus -Pbuilddistro -Pgenerate-static-rpm -Prpm -Ppython-rpm -Prelease-build -P"scala-${SCALA_VERSION}" -P"${SPARK_VERSION}" -P"${HADOOP_VERSION}" -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package"
mvn -B -e -Pci-nexus -Pbuilddistro -Pgenerate-static-rpm -Prpm -Ppython-rpm -Prelease-build -P"scala-${SCALA_VERSION}" -P"${SPARK_VERSION}" -P"${HADOOP_VERSION}" -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package

find . -name *.rpm -not -path './docker/ubuntu/artifacts*' -exec mv -f {} ./docker/centos/artifacts \;