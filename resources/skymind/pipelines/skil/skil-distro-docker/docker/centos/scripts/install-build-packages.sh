#!/usr/bin/env bash

set -euo pipefail

yum -y install centos-release-scl-rh epel-release
yum -y install \
    java-1.8.0-openjdk-devel.x86_64 \
    git \
    rpm-build \
    which \
    redhat-rpm-config

if [ "${PYTHON_VERSION}" == "2" ]; then
      # Install a standard set of Data Science Libraries for user.
     yum -y install python27
else
     yum -y install python36
fi

yum -y group install "Development Tools"

mkdir -p /opt/maven && \
    curl -fsSL http://apache.osuosl.org/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz \
    | tar -xzC /opt/maven --strip-components=1 && ln -s /opt/maven/bin/mvn /usr/bin/mvn

# Cleanup
yum clean all && rm -rf /var/cache/yum