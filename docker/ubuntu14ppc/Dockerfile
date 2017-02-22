FROM ppc64le/ubuntu:14.04

ADD http://www.cmake.org/files/v3.2/cmake-3.2.2.tar.gz /tmp
ADD http://developer.download.nvidia.com/compute/cuda/repos/ubuntu1404/ppc64el/cuda-repo-ubuntu1404_7.5-18_ppc64el.deb /tmp

RUN apt-get update &&\
    apt-get install git build-essential python-software-properties software-properties-common apt-transport-https -y --force-yes &&\
    echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list &&\
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 &&\
    add-apt-repository ppa:openjdk-r/ppa &&\
    cd /tmp && tar xf cmake-3.2.2.tar.gz && cd cmake-3.2.2 && ./configure && make && make install &&\
    dpkg -i /tmp/cuda-repo-ubuntu1404_7.5-18_ppc64el.deb &&\
    rm -f /tmp/cuda-repo-ubuntu1404_7.5-18_ppc64el.deb &&\
    add-apt-repository ppa:ubuntu-toolchain-r/test &&\
    apt-get update &&\
    apt-get install gcc-4.9 g++-4.9 cuda openjdk-8-jdk maven -y --force-yes &&\
    update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java" 1 &&\
    ln -s /usr/lib/jvm/java-8-openjdk-ppc64el/jre/bin/java /etc/alternatives/java &&\
    rm -f /usr/bin/gcc &&\
    rm -f /usr/bin/g++ &&\
    ln -s /usr/bin/gcc-4.9 /usr/bin/gcc &&\
    ln -s /usr/bin/g++-4.9 /usr/bin/g++

# Set user jenkins to the image
RUN useradd -m -d /home/jenkins -s /bin/sh jenkins


# Default command
CMD ["cat"]
