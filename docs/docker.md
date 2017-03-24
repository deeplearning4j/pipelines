# Nvidia docker integration
Nvidia integration to docker made via nvidia-docker-plugin  
https://github.com/NVIDIA/nvidia-docker/wiki/nvidia-docker-plugin  
It just creates docker volume with nvidia-driver (installed earlier).  
Once nvidia-docker-plugin installed and running you may pass nvidia devices and nvidia-drivers to docker container:  
```bash
docker volume ls -f DRIVER=nvidia-docker
docker run --rm -it --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_375.26:/usr/local/nvidia:ro nvidia/cuda /usr/local/nvidia/bin/nvidia-smi
```
# Dockerfile's
Set of Dockerfile's required for deeplearning jenkins builds with docker.  
Linux images based on official Nvidia CUDA images https://hub.docker.com/r/nvidia/cuda/, power image based on https://hub.docker.com/r/ppc64le/ubuntu/  
Images include necessary build tools: gcc, cmake, maven, java, sbt.  
User jenkins added to avoid "I have no name!" with docker-workflow-plugin for jenkins https://wiki.jenkins-ci.org/display/JENKINS/CloudBees+Docker+Pipeline+Plugin  

## Build images manually
```bash
docker build docker/centos6cuda75
```

## Jenkins Builds
Dockerfile's used in jenkins job [BuildDockerImages](jobs/docker/build-push-docker-images.groovy)  
Job can optionally push images to docker registry (deeplearning4j-docker-registry.bintray.io)
