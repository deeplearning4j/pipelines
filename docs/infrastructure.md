# **This file describe infrastructure needed for build   [deeplearning4j](https://github.com/deeplearning4j) project**
## **Project uses following platforms for build**:
- **Linux-x86_64** hosted on [Azure cloud](https://azure.microsoft.com) (FQDN of host: master-jenkins.skymind.io)
- **Android-x86_64** hosted on *Linux x86_64* as container with Android SDK on demand
- **Android arm** hosted on *Linux x86_64* as container with Android SDK on demand
- **Jenkins master** hosted on *Linux x86_64* as a docker-container
- **Nexus Repository PRO** hosted on *Linux x86_64* as a docker-container
- **SonarQube** hosted on *Linux x86_64* as container
- **Artifactory-OSS** hosted on *Linux x86_64* as a docker-container
- **Linux-ppc64le** (PowerPC) hosted on [OSU Open Source Lab](http://osuosl.org/services/powerdev) (FQDN of host: power-jenkins.skymind.io)
- **Windows-x86_64** is hosted on [Azure cloud](https://azure.microsoft.com) (FQDN of host: windows-jenkins.skymind.io)
- **MacOS-x86_64** is hosted on [Macincloud](http://www.macincloud.com) (FQDN of host: mac-jenkins.skymind.io)

Corresponfing labels are assigned to all nodes. Also all nodes should be synchronized on time (NTP is needed).


**linux-x86_64**, **android-x86**, **androi-arm**
Labels: **amd64**, **linux-x86_64**, **android-arm**, **android-x86**, **sshlocal**

Necessary tools/hardware:
 - GPU with latest driver and compatible with CUDA 8.0 & 9.0 ([link](http://www.nvidia.com/Download/index.aspx))
 - GCC 4.9 or 5.x
 - java sdk ([link](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
 - CMake 3.2
 - Maven ([link](https://maven.apache.org/))
 - Git
 - nvidia-docker ([link](https://github.com/NVIDIA/nvidia-docker))
 - Scala ([link](https://www.scala-lang.org))

 Nvidia-docker is for libnd4j builds for different CUDA versions.


#### Connect host as a slave to Jenkins master:

- Go to **Manage Jenkins** -> **Manage Nodes** and click on **New Node**.
Then fill out **Node name** and choose **Permanent Agent**
- Fill out fields:

<p align="center">
  <img src="/imgs/linux-slave.png"/>
</p>

Docker images for CUDA 8.0, CUDA 9.0, Android-x86, Android-arm are built by the job
[BuildDockerImages](http://master-jenkins.skymind.io:8080/job/devel/job/service/job/BuildDockerImages).
This job uses [script](/jobs/docker/build-push-docker-images.groovy):
- Stage **Build** pulls base docker image (the base of our images) from DockerHub registry
CUDA 8.0:
(base image is [nvidia/cuda:8.0-cudnn5-devel-centos6](https://hub.docker.com/r/nvidia/cuda)),
CUDA 9.0:
(base image is [nvidia/cuda:9.0-cudnn7-devel-centos6](https://hub.docker.com/r/nvidia/cuda)),
Android-x86_64 and Android-arm:
(base image is [maven:latest](https://hub.docker.com/_/maven/)) - builds main images according [Dockerfiles](/docker) for each needed platforms.
- Stage **Test** checks docker images by checking the version of necessary tools inside containers.
- Stage **Push** push those images to private Docker registry   ([Bintray](https://bintray.com/deeplearning4j/registry)) when parameter *PUSH_TO_REGISTRY* is chosen.


**Jenkins master**
Jenkins master is hosted on **Linux-x86_64** as a docker container.
Start it with following command:

**docker run -d --name jenkins2322 -p 8080:8080 -p 50000:50000 -v /srv/jenkins:/var/jenkins_home zensam/jenkins:v2.32.2**

where:
- container name - **jenkins2322**
- with **-p** option we expose external 8080 port to 8080 container port and external 50000 port to 50000 container port
- with **-v** option **/srv/jenkins** directory is mounted as volume to mount point **/var/jenkins_home** inside container
- use **zensam/jenkins:v2.32.2** pre-configured Docker image of Jenkins (can be changed to **jenkins:2.32.2**)


~~**`!!ATTENTION!!`**~~
~~Do not upgrade **Docker Pipeline** plugin on Jenkins master from 1.9.1~~ ~~version. New version (at this time 1.10) has some bug which has negative~~ ~~influence to our builds. At least until the moment when this~~ ~~[bug](https://issues.jenkins-ci.org/browse/JENKINS-42322) is fixed.~~

**`!!ATTENTION!!`**
Our current setup suffers from
[bug](https://issues.jenkins-ci.org/browse/JENKINS-29239).

### **Nexus OSS**
Nexus Repository is hosted on **linux-x86_64** as a docker container, start it with following command:

**docker run -d --name nexus -p 8088:8081 -v  /srv/pv/nexus/sonatype-work:/sonatype-work sonatype/nexus:latest**

where:
- assign name of container - **nexus**
- with **-p** option we expose external 8088 port to 8081 port of container
- with **-v** option we mount **/srv/pv/nexus/sonatype-work** directory of jenkins as volume to mount point **/srv/pv/nexus/sonatype-work** inside container
- use **sonatype/nexus:pro** as Docker image of Nexus Pro

The container comes from:
https://hub.docker.com/r/sonatype/nexus3/

### **SonarQube**
SonarQube is hosted on **Linux-x86_64** as a docker container, start it with following command:

**docker run -d --name  sonarqube -p 9000:9000 -p 9092:9092 -v  /srv/pv/sq/extentions:/opt/sonarqube/extensions zensam/sonar**

where:
- assign name of container - **sonarqube**
- with **-p** option we expose external 9000 port to 9000 port of container and external 9092 port to 9092 port of container
- with **-v** option we mount **/srv/pv/sq/extentions** directory of jenkins as volume to mount point **/opt/sonarqube/extensions** inside container
- use **zensam/sonar** as Docker image of SonarQube

### **Artifactory-OSS**
Artifactory-OSS is hosted on **linux-x86_64** as a container, start it with following command:
**docker run -d --name artifactory-oss -p 8081:8081  -v /srv/pv/artifactory/data:/var/opt/jfrog/artifactory/data -v /srv/pv/artifactory/etc:
/var/opt/jfrog/artifactory/etc -v /srv/pv/artifactory/logs:/var/opt/jfrog/artifactory/logs docker.bintray.io/jfrog/artifactory-oss:latest**

where:
- assign name of container - **artifactory-oss**
- with **-p** option we expose external 8081 port to 8081 port of container
- with **-v** option we mount **/srv/pv/artifactory/data** directory of jenkins as volume to mount point **/var/opt/jfrog/artifactory/data** inside container, **/srv/pv/artifactory/etc** directory of jenkins as volume to mount point **/var/opt/jfrog/artifactory/etc** inside container and
**/srv/pv/artifactory/data** directory of jenkins as a **/var/opt/jfrog/artifactory/data** inside container
- use **docker.bintray.io/jfrog/artifactory-oss:latest** as Docker image of Artifactory-OSS

### **Linux-ppc64le**
Labels: **ppc**, **ppc64le**, **ubuntu**

This host also managed by Linux OS, difference only in hardware architecture, some previous steps are also actual for it. For example, process of connecting to Jenkins master is the same:

<p align="center">
  <img src="/imgs/linux-ppc-slave.png"/>
</p>

The process of docker image building is described above and the base image for the PPC platform is [ppc64le/ubuntu:16.04]
(https://hub.docker.com/r/ppc64le/ubuntu). Unlike  **linux-x86_64**, CUDA 8 or CUDA 9 Toolkit and all necessary tools are installed during main docker image building according to [CUDA 8.0 Dockerfile's instruction](/docker/ubuntu16-ppc64le/Dockerfile) or [CUDA 9.0 Dockerfile's instruction](/docker/ubuntu16cuda90-ppc64le/Dockerfile). This node requires only ssh and java sdk for
Jenkins master-slave connection.
Linux-ppc64le slave should be able to build two components:
[libnd4j](https://github.com/deeplearning4j/libnd4j) and [nd4j](https://github.com/deeplearning4j/nd4j).

### **windows-x86_64**
Labels: **windows-x86_64**

Set of tools supposed to installed for Windows related builds for emulating some Linux behaviour on Windows OS family. As on the previous host only two components of project are to be built:
[libnd4j](https://github.com/deeplearning4j/libnd4j) and  [nd4j](https://github.com/deeplearning4j/nd4j).
The following tools are needed:
- Git ([link](https://git-scm.com))
- Maven ([link](https://maven.apache.org))
- java sdk ([link](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
- MSYS2 ([link](http://www.msys2.org))
- CUDA 8.0 and 9.0 Toolkit ([link](https://developer.nvidia.com/cuda-downloads))
- Microsoft Visual Studio Community 2013 ([link](https://www.visualstudio.com/en-us/news/releasenotes/vs2013-community-vs)) or Microsoft Visual Studio Community 2015 ([link](https://www.visualstudio.com/en-us/news/releasenotes/vs2015-archive))
After tools installation system variables inside host must be modified.
- Create new variable *MAVEN_HOME* and *M2_HOME* and set those value as a path to directory with Maven
- Create new variable *JAVA_HOME* and set value as a path to java sdk

<p align="center">
  <img src="/imgs/var_1.png"/>
</p>

- Edit *Path* system variable and path to *C:\msys64\mingw64\bin* and *C:\msys64\usr\bin* in case when MSYS2 is installed in *C:\msys64* and add *C:\\Program Files (x86)\Microsoft Visual Studio  12.0\VC\bin\amd64* in case when Microsoft Visual Studio Community 2013 is installed in
*C:\Program Files (x86)\Microsoft Visual Studio 12.0*

Then connect the host as a slave to Jenkins master:
- Go to **Manage Jenkins** -> **Manage Nodes** and click on **New Node**. Then fill out **Node name** and choose **Permanent Agent**
- Fill out fields:

<p align="center">
  <img src="/imgs/win-slave.png"/>
</p>

- Log in to the host -> edit **C:\Windows\System32\drivers\etc\hosts** and add record **10.0.6.5 master-jenkins.skymind.io** at the end of file. (Workaround for case when **Windows-x86_64** host is to be connected to Jenkins master via internal cloud network
- Open Jenkins in browser (NOTE: Right click on browser icon and click **Run as administrator**) and click **Launch** button

<p align="center">
  <img src="/imgs/win-slave-2.png"/>
</p>

<p align="center">
  <img src="/imgs/jenkins-win-slave.png"/>
</p>

- In opened window click **File** -> **Install as a service**

<p align="center">
  <img src="/imgs/jenkins-service.png"/>
</p>


Now host windows-x86_64 is ready to serve Jenkins jobs.

### **macosx-x86_64**
Labels: **osx**, **macosx**, **macosx-x86_64**

For only two components of the project: [libnd4j](https://github.com/deeplearning4j/libnd4j) and
[nd4j](https://github.com/deeplearning4j/nd4j).
Prepare host for build jobs:
- Host has all the Apple updates installed
- Host has the latest XCode. You will also want to run **xcode-select --install** to ensure the correct version of Xcode is available.
- Host has brew installed(if not get it [brew](http://brew.sh))
- Run **brew install gcc5 cmake** in terminal (this will install GCC). Also please note: openblas is optional, Apple Accelerate library is
supported as well.
- Run **brew install coreutils swig gpg maven**
- Run **brew tap caskroom/drivers**
- Run **brew cask install cuda**
- Add following symbolic links:

```
ln -s /usr/local/Cellar/gcc5/5.4.0/bin/gcc-5 /usr/local/bin/gcc-5
ln -s /usr/local/Cellar/gcc5/5.4.0/bin/g++-5 /usr/local/bin/g++-5
ln -s /usr/local/Cellar/gcc5/5.4.0/bin/gfortran-5 /usr/local/bin/gfortran-5
```
- Optional: Remove gcc-6 by running

```
brew remove gcc
```

#### Connect host as a slave to Jenkins master:

Connect macosx-x86_64 host via ssh or jnlp plugin either:

<h3>ssh agent: </h3>
<p align="center">
  <img src="/imgs/macos-slave-ssh.png"/>
</p>

<h3>jnlp agent: </h3>
<p align="center">
  <img src="/imgs/mac-slave-jlnp.png"/>
</p>

SSH-plugin doesn't require any additional action on the host.
To make JNLP connection persistent - create launchd daemon as described [here] (https://developer.apple.com/library/content/documentation/MacOSX/Conceptual/BPSystemStartup/Chapters/CreatingLaunchdJobs.html)
/Library/LaunchDaemons/org.jenkins-ci.slave.jnlp.plist:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>org.jenkins-ci.slave.jnlp</string>
    <key>ProgramArguments</key>
    <array>
        <string>sudo</string>
        <string>/usr/bin/java</string>
        <string>-Djava.awt.headless=true</string>
        <string>-jar</string>
        <string>/Users/admin/jenkins/slave.jar</string>
        <string>-jnlpUrl</string>
        <string>http://master-jenkins.skymind.io:8080/computer/macosx-x86_64/slave-agent.jnlp</string>
        <string>-secret</string>
        <string>XXXXXXXXXXXXXXXXXXXXXXXXXXXXX_NODE_SECRET_KEY_FROM_NODE_SETTINGS_HERE_XXXXXXXXXXXXXXXXXXXXXXXXXXX</string>
    </array>
    <key>KeepAlive</key>
    <true/>
    <key>RunAtLoad</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/Users/admin/jenkins/stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/admin/jenkins/error.log</string>
</dict>
</plist>
```
```bash
sudo launchctl unload /Library/LaunchDaemons/org.jenkins-ci.slave.jnlp.plist
sudo launchctl load /Library/LaunchDaemons/org.jenkins-ci.slave.jnlp.plist
```
