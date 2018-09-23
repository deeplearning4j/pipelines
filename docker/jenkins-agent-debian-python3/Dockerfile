FROM continuumio/miniconda3

RUN groupadd jenkins -g 1000 && useradd -u 1000 -g 1000 -m jenkins

RUN export DEBIAN_FRONTEND=noninteractive DEBCONF_NONINTERACTIVE_SEEN=true && \
    apt-get update && \
    apt-get -y --no-install-recommends install \
        dirmngr \
        gnupg \
        build-essential \
        ca-certificates \
        software-properties-common && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN add-apt-repository "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886  && \
    apt-get update && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections  && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections  && \
    DEBIAN_FRONTEND=noninteractive DEBCONF_NONINTERACTIVE_SEEN=true && \
    apt-get -y --no-install-recommends install \
        oracle-java8-installer \
        oracle-java8-set-default && \
    apt-get clean  && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN conda config --set always_yes yes --set changeps1 no && \
    conda update -q conda && \
    conda info -a && \
    conda create -q -n test-environment python=3.6 nose

ARG JENKINS_AGENT_VERSION=3.25
RUN curl --create-dirs -sSLo /usr/share/jenkins/slave.jar \
    https://repo.jenkins-ci.org/public/org/jenkins-ci/main/remoting/${JENKINS_AGENT_VERSION}/remoting-${JENKINS_AGENT_VERSION}.jar && \
    chmod 755 /usr/share/jenkins && \
    chmod 644 /usr/share/jenkins/slave.jar

USER jenkins

ENV PATH /opt/conda/envs/test-environment/bin:/home/jenkins/.local/bin:$PATH

ARG JENKINS_AGENT_WORKDIR=/home/jenkins/agent
ENV JENKINS_AGENT_WORKDIR=${JENKINS_AGENT_WORKDIR}
RUN mkdir /home/jenkins/.jenkins && mkdir -p ${JENKINS_AGENT_WORKDIR}

VOLUME /home/jenkins/.jenkins
VOLUME ${JENKINS_AGENT_WORKDIR}

COPY jenkins-slave /usr/local/bin/jenkins-slave

RUN echo "source activate test-environment" >> ~/.bashrc

WORKDIR /home/jenkins

ENTRYPOINT ["jenkins-slave"]