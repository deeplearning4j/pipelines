FROM nvidia/cuda:7.5-cudnn5-devel-ubuntu14.04

ENV JAVA_ARCH_DOWNLOAD_URL=http://download.oracle.com/otn-pub/java/jdk/8u121-b13/e9e7ea248e2c4826b92b3f075a80e441/jdk-8u121-linux-x64.tar.gz \
	JAVA_HOME=/opt/java \
	PATH=/opt/sbt/bin:/opt/cmake/bin:${PATH} \
	MAVEN_VERSION=3.3.9

RUN apt-get update && apt-get -y --no-install-recommends install curl ca-certificates software-properties-common git && \
	add-apt-repository -y ppa:ubuntu-toolchain-r/test && \
	apt-get update && \
	apt-get install -y --no-install-recommends gcc-4.9 g++-4.9 && \
	update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.9 50 && \
	update-alternatives --install /usr/bin/g++ g++ /usr/bin/g++-4.9 50 && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir -p /opt && \
	curl -H "Cookie: oraclelicense=accept-securebackup-cookie" \
	-fsSL "${JAVA_ARCH_DOWNLOAD_URL}" \
	| tar -xzC /opt && \
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

RUN mkdir -p /opt/cmake && \
	curl -fsSL https://cmake.org/files/v3.7/cmake-3.7.2-Linux-x86_64.tar.gz \
	| tar -xzC /opt/cmake --strip-components=1

#RUN groupadd jenkins -g JENKINSGID && useradd -u JENKINSUID -g JENKINSGID -m jenkins
