FROM centos:7

RUN yum -y clean all && \
        yum -y install \
            centos-release-scl-rh \
            epel-release \
           && \
        yum -y install \
            devtoolset-4-toolchain \
            devtoolset-4-libasan-devel \
            devtoolset-4-liblsan-devel \
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
            sudo \
            # Required for libnd4j CPU tests (minifier)
            which \
            # Required for Datavec NativeImageLoader
            gtk2-devel \
           && \
        yum clean all

ENV JAVA_HOME /opt/java
RUN mkdir -p /opt && \
    # Switch from fecth jdk with curl to wget, because of errors during the fetch
    wget --no-cookies --no-check-certificate \
        --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
        http://download.oracle.com/otn-pub/java/jdk/8u191-b12/2787e4a523244c269598db4e85c51e0c/jdk-8u191-linux-x64.tar.gz && \
    tar -xzf jdk-8u191-linux-x64.tar.gz -C /opt && \
    rm jdk-8u191-linux-x64.tar.gz && \
    mv /opt/jdk* ${JAVA_HOME} && \
    update-alternatives --install /usr/bin/java java ${JAVA_HOME}/bin/java 100 && \
    update-alternatives --install /usr/bin/javac javac ${JAVA_HOME}/bin/javac 100

ENV M2_HOME /opt/maven
RUN mkdir -p /opt/maven && \
    curl -fsSL http://apache.osuosl.org/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz \
    | tar -xzC /opt/maven --strip-components=1 && \
    ln -s /opt/maven/bin/mvn /usr/bin/ && \
    # Workaround for concurrent safe maven local repository
    curl -O http://repo1.maven.org/maven2/io/takari/aether/takari-local-repository/0.11.2/takari-local-repository-0.11.2.jar && \
    mv takari-local-repository-0.11.2.jar ${M2_HOME}/lib/ext && \
    curl -O http://repo1.maven.org/maven2/io/takari/takari-filemanager/0.8.3/takari-filemanager-0.8.3.jar && \
    mv takari-filemanager-0.8.3.jar ${M2_HOME}/lib/ext

RUN mkdir -p /opt/sbt && \
    curl -fsSL https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz \
    | tar -xzC /opt/sbt --strip-components=1

RUN mkdir -p /opt/cmake && \
    curl -fsSL https://cmake.org/files/v3.9/cmake-3.9.0-Linux-x86_64.tar.gz \
    | tar -xzC /opt/cmake --strip-components=1

ENV PROTOBUF_HOME /opt/protobuf
RUN source /opt/rh/devtoolset-6/enable && \
    curl -fsSL https://github.com/google/protobuf/releases/download/v3.5.1/protobuf-cpp-3.5.1.tar.gz \
    | tar xz && \
    cd protobuf-3.5.1 && \
    ./configure --prefix=${PROTOBUF_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -r protobuf-3.5.1

# Install git lfs for dl4j-test-resources
#RUN curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.rpm.sh | bash && \
#    yum -y clean all && \
#        yum -y install \
#            git-lfs \
#            && \
#        yum clean all && \
#        git lfs install

ENV HOME /home/jenkins
RUN groupadd jenkins -g 1000 && \
    useradd -d $HOME -u 1000 -g 1000 -m jenkins && \
    echo "jenkins ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/user && \
    chmod 0440 /etc/sudoers.d/user

ARG JENKINS_AGENT_VERSION=3.27
RUN curl --create-dirs -sSLo /usr/share/jenkins/slave.jar \
    https://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/${JENKINS_AGENT_VERSION}/remoting-${JENKINS_AGENT_VERSION}.jar && \
    chmod 755 /usr/share/jenkins && \
    chmod 644 /usr/share/jenkins/slave.jar

USER jenkins

ARG JENKINS_AGENT_WORKDIR=/home/jenkins/agent
ENV JENKINS_AGENT_WORKDIR=${JENKINS_AGENT_WORKDIR}
RUN mkdir /home/jenkins/.jenkins && mkdir -p ${JENKINS_AGENT_WORKDIR}

VOLUME /home/jenkins/.jenkins
VOLUME ${JENKINS_AGENT_WORKDIR}

WORKDIR /home/jenkins

ENV PATH /opt/sbt/bin:/opt/cmake/bin:${PROTOBUF_HOME}/bin:${PATH}

COPY jenkins-slave /usr/local/bin/jenkins-slave

ENTRYPOINT ["jenkins-slave"]
