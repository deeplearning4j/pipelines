#!/usr/bin/env bash
set -e

find ./docker/centos/artifacts -name 'skil-*.rpm' -exec yum install -y  {} \;

su skil -c 'source /etc/skil/skil-env.sh; java -cp "/opt/skil/lib/*:/opt/skil/logback/*" io.skymind.zeppelin.main.InstallInterpreters'

su skil -c "mvn -B -e -s ${MAVEN_SETTINGS} -Pci-nexus -Pbuilddistro -Pgenerate-static-rpm -Prpm -Ppython-rpm -Prelease-build -DskipTests=true -Dmaven.test.skip=true -Dmaven.javadoc.skip=true package"

find . -name *.rpm -not -path './docker/centos/artifacts*' -exec mv -f {} ./docker/centos/artifacts \;