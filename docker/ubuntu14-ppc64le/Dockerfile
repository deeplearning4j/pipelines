FROM ppc64le/ubuntu:14.04

RUN apt-get update && \
    apt-get install build-essential python-software-properties software-properties-common curl git -y --force-yes --no-install-recommends && \
    add-apt-repository ppa:openjdk-r/ppa -y && \
    add-apt-repository ppa:ubuntu-toolchain-r/test -y && \
    curl -fsSL http://developer.download.nvidia.com/compute/cuda/7.5/Prod/local_installers/cuda-repo-ubuntu1404-7-5-local_7.5-18_ppc64el.deb -o /tmp/cuda-repo.deb && \
    dpkg -i /tmp/cuda-repo.deb && \
    rm -f /tmp/cuda-repo.deb && \
    apt-get update && \
    apt-get install gcc-4.9 g++-4.9 cuda openjdk-8-jdk maven -y --force-yes --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java" 1 && \
    rm -f /etc/alternatives/java /usr/bin/gcc /usr/bin/g++ && \
    ln -s /usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java /etc/alternatives/java && \
    ln -s /usr/bin/gcc-4.9 /usr/bin/gcc && \
    ln -s /usr/bin/g++-4.9 /usr/bin/g++

RUN curl -fsSL http://www.cmake.org/files/v3.7/cmake-3.7.2.tar.gz -o /tmp/cmake-3.7.2.tar.gz && \
    tar xf /tmp/cmake-3.7.2.tar.gz -C /tmp && \
    cd /tmp/cmake-3.7.2 && \
    ./configure && make && make install && \
    rm -rf /tmp/*

# Set user jenkins to the image
RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

# Default command
CMD ["cat"]
