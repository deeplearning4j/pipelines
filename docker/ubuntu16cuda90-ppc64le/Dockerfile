FROM ppc64le/ubuntu:16.04

# Set DEBIAN_FRONTEND to skip any interactive post-install configuration steps
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get -qqy update && \
    apt-get -qqy install --no-install-recommends \
        build-essential \
        python-software-properties \
        software-properties-common \
        curl \
        git \
        wget \
        openssl \
        && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/7fa2af80.pub && \
    curl -fsSL http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/cuda-repo-ubuntu1604_9.0.176-1_ppc64el.deb -o /tmp/cuda-repo.deb && \
    dpkg -i /tmp/cuda-repo.deb && \
    rm -f /tmp/cuda-repo.deb

ENV CMAKE_PACKAGE_VERSION 3.9.0
ENV CMAKE_PACKAGE_NAME cmake-${CMAKE_PACKAGE_VERSION}.tar.gz
ENV CMAKE_DOWNLOAD_URL https://cmake.org/files/v3.9/${CMAKE_PACKAGE_NAME}
ENV CMAKE_HOME /opt/cmake

RUN curl -fsSL ${CMAKE_DOWNLOAD_URL} \
    | tar xz && \
    cd cmake-${CMAKE_PACKAGE_VERSION} && \
    ./configure --prefix=${CMAKE_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -r cmake-${CMAKE_PACKAGE_VERSION}

ENV PROTOBUF_PACKAGE_VERSION 3.5.0
ENV PROTOBUF_PACKAGE_NAME protobuf-cpp-${PROTOBUF_PACKAGE_VERSION}.tar.gz
ENV PROTOBUF_DOWNLOAD_URL https://github.com/google/protobuf/releases/download/v${PROTOBUF_PACKAGE_VERSION}/${PROTOBUF_PACKAGE_NAME}
ENV PROTOBUF_HOME /opt/protobuf

RUN curl -fsSL ${PROTOBUF_DOWNLOAD_URL} \
    | tar xz && \
    cd protobuf-${PROTOBUF_PACKAGE_VERSION} && \
    ./configure --prefix=${PROTOBUF_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -r protobuf-${PROTOBUF_PACKAGE_VERSION}

ENV CUDA_VERSION 9-0
RUN apt-get -qqy update && \
    apt-get -qqy install --no-install-recommends \
        gcc \
        g++ \
        cuda-${CUDA_VERSION} \
        openjdk-8-jdk-headless \
        maven \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java" 100 && \
    rm -f /etc/alternatives/java /usr/bin/gcc /usr/bin/g++ && \
    ln -s /usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java /etc/alternatives/java && \
    ln -s /usr/bin/gcc-5 /usr/bin/gcc && \
    ln -s /usr/bin/g++-5 /usr/bin/g++ && \
    ln -s ${CMAKE_HOME}/bin/* /usr/local/bin && \
    ln -s ${PROTOBUF_HOME}/bin/* /usr/local/bin

# Install go dependency for git lfs
RUN curl -fsSL "https://dl.google.com/go/go1.10.2.linux-ppc64le.tar.gz" \
    | tar -xzC /opt && \
    ln -s /opt/go/bin/* /usr/local/bin

# Install git lfs for dl4j-test-resources
RUN curl -fsSL "https://github.com/git-lfs/git-lfs/archive/v2.4.0.tar.gz" \
    | tar -xzC /opt && \
    cd /opt/git-lfs-2.4.0 && \
    ./script/bootstrap && \
    ln -s /opt/git-lfs-2.4.0/bin/* /usr/local/bin && \
    git lfs install --system

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

ENV PATH=/usr/local/cuda-9.0/bin${PATH:+:${PATH}} \
    LD_LIBRARY_PATH=/usr/local/cuda-9.0/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}

USER jenkins

CMD ["cat"]
