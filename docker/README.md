## build-docker-images.groovy
Builds local images based on https://hub.docker.com/r/nvidia/cuda/ with tools gcc, cmake, maven, sbt. Add user jenkins to avoid "I have no name!" in running with docker-workflow-plugin containers.  
List of images:  
 - ubuntu14cuda75 - FROM nvidia/cuda:7.6-cudnn5-devel-ubuntu14.04  
 - ubuntu14cuda80 - FROM nvidia/cuda:8.0-cudnn5-devel-ubuntu14.04  
 - centos6cuda7 - FROM nvidia/cuda:7.5-cudnn5-devel-centos6  
 - centos6cuda80 - FROM nvidia/cuda:8.0-cudnn5-devel-centos6  

In future it could be updated with automatic push to skymind docker registry.  

## prepare-docker-images.groovy
Builds local images based on https://hub.docker.com/r/intropro/nvidia-docker/. Add user jenkins to avoid "I have no name!" in running with docker-workflow-plugin containers.  
It builds the same images as above.  
List of images:  
 - ubuntu14cuda75 - FROM intropro/nvidia-docker:7.6-cudnn5-devel-ubuntu14.04  
 - ubuntu14cuda80 - FROM intropro/nvidia-docker:8.0-cudnn5-devel-ubuntu14.04  
 - centos6cuda7 - FROM intropro/nvidia-docker:7.5-cudnn5-devel-centos6  
 - centos6cuda80 - FROM intropro/nvidia-docker:8.0-cudnn5-devel-centos6  


## show-aws-ec2-instances.groovy
Shows all non-terminated instances deployed with "tag:DeployedBy,Values=JenkinsEc2Plugin,JenkinsWithTerraform", the tags used in Jenkins.  
