FROM docker:18.06.1-ce-dind

RUN apk add --no-cache \
        py-pip \
        openssl \
        bash \
        shadow

RUN pip install docker-compose

#ENV HOME /home/jenkins
#RUN addgroup -g 1000 jenkins && \
#    adduser -D -u 1000 -G jenkins jenkins && \
#    usermod -aG ping jenkins
#
#USER jenkins
#WORKDIR $HOME
#
#CMD ["cat"]