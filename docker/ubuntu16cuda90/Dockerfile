FROM nvidia/cuda:9.0-cudnn7-devel-ubuntu16.04

ENV JAVA_PACKAGE_NAME=jdk-8u131-linux-x64.tar.gz \
    JAVA_ARCH_DOWNLOAD_URL=http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163 \
	JAVA_HOME=/opt/java \
	PATH=/opt/sbt/bin:${PATH} \
	MAVEN_VERSION=3.3.9

RUN apt-get update && \
    apt-get -y --no-install-recommends install wget curl ca-certificates software-properties-common git && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir -p /opt && \
	wget --no-cookies --no-check-certificate \
	    --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
	    "${JAVA_ARCH_DOWNLOAD_URL}/${JAVA_PACKAGE_NAME}" && \
    tar -xzf "${JAVA_PACKAGE_NAME}" -C /opt && \
    rm "${JAVA_PACKAGE_NAME}" && \
	mv /opt/jdk* /opt/java && \
	update-alternatives --install /usr/bin/java java /opt/java/bin/java 100 && \
	update-alternatives --install /usr/bin/javac javac /opt/java/bin/javac 100

RUN mkdir -p /opt/maven && \
	curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /opt/maven --strip-components=1 && \
	ln -s /opt/maven/bin/mvn /usr/bin/mvn

RUN mkdir -p /opt/sbt && \
	curl -fsSL https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz \
	| tar -xzC /opt/sbt --strip-components=1

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins
