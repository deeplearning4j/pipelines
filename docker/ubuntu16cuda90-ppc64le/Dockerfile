FROM ppc64le/ubuntu:16.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get -qqy update && \
    apt-get -qqy install build-essential python-software-properties software-properties-common curl git --no-install-recommends && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    apt-key adv --fetch-keys http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/7fa2af80.pub && \
    curl -fsSL http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1604/ppc64el/cuda-repo-ubuntu1604_9.0.176-1_ppc64el.deb -o /tmp/cuda-repo.deb && \
    dpkg -i /tmp/cuda-repo.deb && \
    rm -f /tmp/cuda-repo.deb

RUN curl -fsSL https://cmake.org/files/v3.9/cmake-3.9.0.tar.gz \
    | tar xz && \
    cd cmake-3.9.0 && \
    ./configure --prefix=/opt/cmake && \
    make && make install

RUN apt-get -qqy update && \
    apt-get -qqy install gcc g++ cuda openjdk-8-jdk-headless maven --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java" 1 && \
    rm -f /etc/alternatives/java /usr/bin/gcc /usr/bin/g++ && \
    ln -s /usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java /etc/alternatives/java && \
    ln -s /usr/bin/gcc-5 /usr/bin/gcc && \
    ln -s /usr/bin/g++-5 /usr/bin/g++ && \
    ln -s /opt/cmake/bin/* /usr/local/bin

# Set user jenkins to the image
RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

# Default command
CMD ["cat"]
