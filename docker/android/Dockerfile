FROM nvidia/cuda:8.0-cudnn5-devel-centos6

ENV JAVA_ARCH_DOWNLOAD_URL=http://download.oracle.com/otn/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.tar.gz \
        JAVA_HOME=/opt/java \
        PATH=/opt/sbt/bin:/opt/cmake/bin:${PATH} \
        MAVEN_VERSION=3.3.9

RUN yum -y clean all && \
        yum -y install centos-release-scl-rh epel-release && \
        yum -y install devtoolset-3-toolchain tar curl openssl ca-certificates git rpm-build && \
        yum clean all

RUN mkdir -p /opt && \
        curl -j -k -L -H "Cookie: oraclelicense=accept-securebackup-cookie" \
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

#FROM maven:latest
#
#RUN apt-get update &&\
#    apt-get install git g++ gcc make -y --force-yes --no-install-recommends && \
#    apt-get clean && \
#    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
#
#RUN mkdir -p /opt/cmake && \
#    curl -fsSL https://cmake.org/files/v3.7/cmake-3.7.2-Linux-x86_64.tar.gz | tar -xzC /opt/cmake --strip-components=1

ENV ANDROID_NDK_VERSION r13b
ENV ANDROID_NDK_URL=http://dl.google.com/android/repository/android-ndk-$ANDROID_NDK_VERSION-linux-x86_64.zip \
    ANDROID_NDK_HOME=/opt/android-ndk-$ANDROID_NDK_VERSION \
    ANDROID_NDK=/opt/android-ndk-r13b

RUN curl -fsSL "$ANDROID_NDK_URL" -o android-ndk-$ANDROID_NDK_VERSION-linux-x86_64.zip && \
    unzip -q android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip -d /opt && \
    rm -f android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip

ENV PATH /opt/cmake/bin:$ANDROID_NDK_HOME:$PATH

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

CMD ["cat"]
