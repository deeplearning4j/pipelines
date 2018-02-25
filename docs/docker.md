# Nvidia docker integration
Nvidia integration to docker made with nvidia-docker-plugin
https://github.com/NVIDIA/nvidia-docker/wiki/nvidia-docker-plugin and jenkins docker-workflow-plugin  https://wiki.jenkins-ci.org/display/JENKINS/CloudBees+Docker+Pipeline+Plugin.
Nvidia-docker-plugin just creates docker volume with nvidia-driver (installed earlier).
Once nvidia-docker-plugin installed and running you may pass nvidia devices and nvidia-drivers to docker container:
```bash
docker volume ls -f DRIVER=nvidia-docker
docker run --rm -it --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_375.26:/usr/local/nvidia:ro nvidia/cuda /usr/local/nvidia/bin/nvidia-smi
```

On each upgrade, note that you will have to re-execute the creation of the
nvidia-docker volume exposing the driver's contents to our Docker images:

``` bash
docker volume create --driver=nvidia-docker --name=nvidia_driver_$(modinfo -F version nvidia)
```

We have created the following image in a
`/etc/systemd/system/nvidia-volume.service` unit:

``` bash
[Unit]
Description=Nvidia volume for nvidia-docker container management

[Service]
ExecStart=/bin/bash -c 'docker volume create --driver=nvidia-docker --name=nvidia_driver_$(modinfo -F version nvidia)'

[Install]
WantedBy=multi-user.target
```
At reinstall of the CI server, it may need to be started and enabled again.

# Dockerfile's
Set of Dockerfile's required for deeplearning jenkins builds with docker.
Linux images based on official Nvidia CUDA images https://hub.docker.com/r/nvidia/cuda/, power image based on https://hub.docker.com/r/ppc64le/ubuntu/
Images include necessary build tools: gcc, cmake, maven, java, sbt.
User jenkins in Dockerfile should have same id with your jenkins-slave user.

## Build images manually
```bash
docker build docker/centos6cuda80
```

## Jenkins Builds
Dockerfile's used in jenkins job [BuildDockerImages](jobs/docker/build-push-docker-images.groovy)
Job can optionally push images to docker registry (deeplearning4j-docker-registry.bintray.io)
