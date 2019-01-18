ARG SKIL_BASE_IMAGE_NAME
FROM $SKIL_BASE_IMAGE_NAME

ARG SKIL_VERSION
ARG SKIL_DOCKER_IMAGE_REVISION
ARG GIT_COMMIT

LABEL maintainer="Skymind Inc <help@skymind.io>" \
    ai.skymind.skil.description="SKIL Docker container" \
    ai.skymind.skil.version="${SKIL_VERSION}" \
    ai.skymind.skil.git.commit.id="${GIT_COMMIT}" \
    ai.skymind.skil.docker.image.revision="${SKIL_DOCKER_IMAGE_REVISION}"

ENV SKIL_HOME=/opt/skil \
    TMP_FOLDER=/tmp

RUN useradd -r -M -d /opt/skil -s /sbin/nologin -c "SKIL Daemon" skil && \
        usermod -a -G skil daemon && \
        mkdir -p ${SKIL_HOME} && chown skil:daemon ${SKIL_HOME}

RUN yum -y install \
        centos-release-scl-rh \
        epel-release \
     && \
    yum -y install \
        java-1.8.0-openjdk-devel.x86_64 \
        openssl \
        ca-certificates \
        git \
        which \
     && \
    yum clean all \
     && \
    rm -rf /var/cache/yum

COPY ./artifacts/*.rpm ${TMP_FOLDER}/

WORKDIR ${TMP_FOLDER}

RUN yum -y install \
        skil-server-*.rpm \
     && \
    yum clean all && rm -rf /var/cache/yum && rm -rf ${TMP_FOLDER}/*.rpm \
     && \
    mkdir -p /run/skil && chown -R skil:daemon /run/skil

COPY ./scripts/install-skil-python.sh ${TMP_FOLDER}/scripts/
RUN chmod +x ${TMP_FOLDER}/scripts/install-skil-python.sh && \
        ${TMP_FOLDER}/scripts/install-skil-python.sh && \
        rm -f ${TMP_FOLDER}/scripts/install-skil-python.sh

USER skil

WORKDIR ${SKIL_HOME}

ENV PATH=/opt/skil/sbin:/opt/skil/miniconda/bin:${PATH}

COPY --chown=skil:daemon ./scripts/start-skil.sh ${SKIL_HOME}/sbin/start-skil.sh

# DL4J UI first port
EXPOSE 8080
# PLD port
EXPOSE 9008

ENTRYPOINT ["start-skil.sh", "--foreground=True"]
