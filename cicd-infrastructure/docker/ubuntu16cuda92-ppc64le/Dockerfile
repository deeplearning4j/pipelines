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
        # Required for Datavec NativeImageLoader
        libgtk2.0-dev \
        && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/7fa2af80.pub && \
    curl -fsSL http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/cuda-repo-ubuntu1604_9.2.88-1_ppc64el.deb -o /tmp/cuda-repo.deb && \
    dpkg -i /tmp/cuda-repo.deb && \
    rm -f /tmp/cuda-repo.deb

ENV CMAKE_HOME /opt/cmake
RUN curl -fsSL https://cmake.org/files/v3.9/cmake-3.9.0.tar.gz \
    | tar xz && \
    cd cmake-3.9.0 && \
    ./configure --prefix=${CMAKE_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -r cmake-3.9.0

ENV PROTOBUF_HOME /opt/protobuf
RUN curl -fsSL https://github.com/google/protobuf/releases/download/v3.5.1/protobuf-cpp-3.5.1.tar.gz \
    | tar xz && \
    cd protobuf-3.5.1 && \
    ./configure --prefix=${PROTOBUF_HOME} && \
    make -j2 && \
    make install && \
    cd .. && \
    rm -r protobuf-3.5.1

ENV CUDA_VERSION 9-2
RUN apt-get -qqy update && \
    apt-get -qqy install --no-install-recommends \
        gcc-7 \
        g++-7 \
        cuda-${CUDA_VERSION} \
        openjdk-8-jdk-headless \
        maven \
        && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java" 100 && \
    rm -f /etc/alternatives/java /usr/bin/gcc /usr/bin/g++ && \
    ln -s /usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java /etc/alternatives/java && \
    ln -s /usr/bin/gcc-7 /usr/bin/gcc && \
    ln -s /usr/bin/g++-7 /usr/bin/g++ && \
    ln -s ${CMAKE_HOME}/bin/* /usr/local/bin && \
    ln -s ${PROTOBUF_HOME}/bin/* /usr/local/bin

# Install go dependency for git lfs
RUN curl -fsSL "https://dl.google.com/go/go1.10.2.linux-ppc64le.tar.gz" \
    | tar -xzC /opt && \
    ln -s /opt/go/bin/* /usr/local/bin

# Install git lfs for dl4j-test-resources
#RUN curl -fsSL "https://github.com/git-lfs/git-lfs/archive/v2.4.0.tar.gz" \
#    | tar -xzC /opt && \
#    cd /opt/git-lfs-2.4.0 && \
#    ./script/bootstrap && \
#    ln -s /opt/git-lfs-2.4.0/bin/* /usr/local/bin && \
#    git lfs install --system

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

ENV PATH=/usr/local/cuda-9.2/bin${PATH:+:${PATH}} \
    LD_LIBRARY_PATH=/usr/local/cuda-9.2/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}

USER jenkins

CMD ["cat"]
