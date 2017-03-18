### Jenkins pipeline for deeplearning4j components
* **libnd4j build power8 job**  
   http://ec2-54-202-57-224.us-west-2.compute.amazonaws.com:8080/job/Pipelines/job/power8-buid-libnd4j/  
  run pipeline build script from github(github.com/deeplearning4j/pipelines/jobs/power8-02-libnd4j.groovy)  
  build:
   * _./buildnativeoperations.sh -c cuda -сс 30_
     * '-c' sets GPU type 
     *  '-cc' architecture version of nvidia GPU

   Required **_ubuntu_cuda_ready:14.04_** docker image which available on power8 build agent(140.211.168.159)


#### Pre-requisitgies

#### Pre-requisitgies  
##### [Jenkins Plugins](./plugins.md)
