FROM ppc64le/ubuntu:16.04

# Set DEBIAN_FRONTEND to skip any interactive post-install configuration steps
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get -qqy update && \
    apt-get -qqy install --no-install-recommends \
        build-essential \
        software-properties-common \
        curl \
        git \
        wget \
        openssl \
	    dirmngr \
	    gnupg-agent \
        # Required for Datavec NativeImageLoader
        libgtk2.0-dev \
        && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1804/ppc64el/7fa2af80.pub && \
    curl -fsSL https://developer.nvidia.com/compute/cuda/10.0/Prod/local_installers/cuda-repo-ubuntu1604-10-0-local-10.0.130-410.48_1.0-1_ppc64el -o /tmp/cuda-repo.deb && \
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

ENV CUDA_VERSION 10-0
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
#RUN curl -fsSL "https://github.com/git-lfs/git-lfs/archive/v2.4.0.tar.gz" \
#    | tar -xzC /opt && \
#    cd /opt/git-lfs-2.4.0 && \
#    ./script/bootstrap && \
#    ln -s /opt/git-lfs-2.4.0/bin/* /usr/local/bin && \
#    git lfs install --system

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

ENV PATH=/usr/local/cuda-10.0/bin${PATH:+:${PATH}} \
    LD_LIBRARY_PATH=/usr/local/cuda-10.0/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}

USER jenkins

CMD ["cat"]
