FROM maven:latest

RUN apt-get update &&\
    apt-get install git g++ gcc make -y --force-yes --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir -p /opt/cmake && \
    curl -fsSL https://cmake.org/files/v3.7/cmake-3.7.2-Linux-x86_64.tar.gz | tar -xzC /opt/cmake --strip-components=1

ENV ANDROID_NDK_VERSION r13b
ENV ANDROID_NDK_URL=http://dl.google.com/android/repository/android-ndk-$ANDROID_NDK_VERSION-linux-x86_64.zip \
    ANDROID_NDK_HOME=/opt/android-ndk-$ANDROID_NDK_VERSION \
    ANDROID_NDK=/opt/android-ndk-r13b

RUN curl -fsSL "$ANDROID_NDK_URL" -o android-ndk-$ANDROID_NDK_VERSION-linux-x86_64.zip && \
    unzip android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip -d /opt && \
    rm -f android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64.zip

ENV PATH /opt/cmake/bin:$ANDROID_NDK_HOME:$PATH

RUN groupadd jenkins -g 500 && useradd -u 500 -g 500 -m jenkins

CMD ["cat"]
