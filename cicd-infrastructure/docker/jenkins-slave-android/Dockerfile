ARG BASEIMAGE_VERSION=6.6
FROM amd64/centos:${BASEIMAGE_VERSION} as BASEIMAGE

FROM BASEIMAGE AS BUILD_TOOLS

RUN yum install -y \
        centos-release-scl-rh \
        epel-release && \
    yum update -y && \
    yum install -y \
        devtoolset-6-toolchain \
        devtoolset-6-libasan-devel \
        devtoolset-6-liblsan-devel \
        tar \
        curl \
        openssl \
        ca-certificates \
        unzip && \
    yum clean all && rm -rf /var/cache/yum

ENV M2_HOME /opt/maven
RUN mkdir -p /opt/maven && \
    curl -fsSL http://apache.osuosl.org/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz | \
        tar -xzC /opt/maven --strip-components=1 && \
    # Workaround for concurrent safe maven local repository
    curl -O http://repo1.maven.org/maven2/io/takari/aether/takari-local-repository/0.11.2/takari-local-repository-0.11.2.jar && \
    mv takari-local-repository-0.11.2.jar ${M2_HOME}/lib/ext && \
    curl -O http://repo1.maven.org/maven2/io/takari/takari-filemanager/0.8.3/takari-filemanager-0.8.3.jar && \
    mv takari-filemanager-0.8.3.jar ${M2_HOME}/lib/ext

RUN mkdir -p /opt/sbt && \
    curl -fsSL https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz | \
    tar -xzC /opt/sbt --strip-components=1

RUN mkdir -p /opt/cmake && \
    curl -fsSL https://cmake.org/files/v3.9/cmake-3.9.0-Linux-x86_64.tar.gz | \
    tar -xzC /opt/cmake --strip-components=1

ENV PROTOBUF_HOME /opt/protobuf
RUN source /opt/rh/devtoolset-6/enable && \
    curl -fsSL https://github.com/google/protobuf/releases/download/v3.5.1/protobuf-cpp-3.5.1.tar.gz \
    | tar xz && \
    cd protobuf-3.5.1 && \
    ./configure --prefix=${PROTOBUF_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -rf protobuf-3.5.1

ENV ANDROID_NDK_HOME /opt/android-ndk-r16b
RUN mkdir -p /opt && \
    curl -s http://dl.google.com/android/repository/android-ndk-r16b-linux-x86_64.zip -o android-ndk-r16b-linux-x86_64.zip && \
    unzip -q android-ndk-r16b-linux-x86_64.zip -d /opt && \
    rm -f android-ndk-r16b-linux-x86_64.zip

FROM BASEIMAGE AS BASE_BUILDER_IMAGE

COPY --from=BUILD_TOOLS /opt /opt

RUN yum install -y \
        centos-release-scl-rh \
        epel-release && \
    yum update -y && \
    yum install -y \
        devtoolset-6-toolchain \
        devtoolset-6-libasan-devel \
        devtoolset-6-liblsan-devel \
        devtoolset-7-toolchain \
        devtoolset-7-libasan-devel \
        devtoolset-7-liblsan-devel \
        tar \
        wget \
        curl \
        openssl \
        ca-certificates \
        git \
        rpm-build \
        java-1.8.0-openjdk-devel \
        # Required for libnd4j CPU tests (minifier)
        which \
        # Required for Datavec NativeImageLoader
        gtk2-devel && \
    yum clean all && rm -rf /var/cache/yum && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn

ENV HOME /home/jenkins
# FIXME: WORKAROUND FOR WRONG JENKINS USER ID IN JNLP CONTAINER
RUN groupadd jenkins -g 10000 && useradd -d $HOME -u 10000 -g 10000 -m jenkins

USER jenkins

WORKDIR ${HOME}

ENV PATH=/opt/sbt/bin:/opt/cmake/bin:${PROTOBUF_HOME}/bin:${PATH} \
    JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap ${JAVA_OPTS}" \
    # This environment variable required for buildnativeoperations.sh script execution
    ANDROID_NDK=${ANDROID_NDK_HOME}

CMD ["cat"]






