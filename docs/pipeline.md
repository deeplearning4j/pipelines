# Jenkins pipeline for deeplearning4j components
## **RELEASE**  
Start all-multiplatform job for build all platforms in parallel to release components. Upload all libraries to staging repository, make tag and push it to github.  
Link: http://master-jenkins.skymind.io:8080/job/dl4j/job/RELEASE  
Job parameters:  
* PLATFORMS:  
    **desc:** Build libraries on one or several platforms in parallel, linux-x86_64 and android* platforms can be built on same jenkins linux node, windows-x86_64, linux-ppc64le and macosx-x86_64 should be built on their native jenkins nodes (nodes choices automatically by labels). If any platform unchecked - RELEASE won't be successful because of nd4s dependency  of all platforms.  
    **available values:** linux-x86_64, linux-ppc64le, android-arm, android-x86, macosx-x86_64, windows-x86_64  
    **default value:** linux-x86_64, linux-ppc64le, android-arm, android-x86, macosx-x86_64, windows-x86_64  
* VERSION:  
    **desc:** Deeplearning component release version  
    **default value:** 0.8.1  
* SKIP_TEST  
    **desc:** Skip tests during mvn execution (ex: mvn clean deploy -Dmaven.test.skip=true)  
    **default value:** ON  
* SONAR  
    **desc:** Check code with SonarQube  
    **default value:** OFF  
* GIT_BRANCHNAME  
    **desc:** Git branch for project  
    **default value:** master  
* PROFILE_TYPE  
    **desc:** Profile type  
    **available values:** nexus, jfrog, bintray, sonatype  
* CBUILD  
    **desc:** Development option, it is not recommended to uncheck it. If unchecked it will try to find libnd4j from snapshot repository instead of building it  
    **default value:** ON  
* TAG  
    **desc:** push tags to GitHub  
    **default value:** OFF  

## **SNAPSHOT**  
Start all-multiplatform job for build all platforms in parallel to build components.  
Basically it is same to RELEASE excluding upload to snapshot repository and git tagging.  
link: http://master-jenkins.sJob parameters:  
* PLATFORMS:  
    **desc:** Build libraries on one or several platforms in parallel, linux-x86_64 and android* platforms can be built on same jenkins linux node, windows-x86_64, linux-ppc64le and macosx-x86_64 should be built on their native jenkins nodes (nodes choices automatically by labels). If any platform unchecked - RELEASE won't be successful because of nd4s dependency  of all platforms.  
    **available values:** linux-x86_64, linux-ppc64le, android-arm, android-x86, macosx-x86_64, windows-x86_64  
    **default value:** linux-x86_64, linux-ppc64le, android-arm, android-x86, macosx-x86_64, windows-x86_64  
* VERSION:  
    **desc:** Deeplearning component snapshot version  
    **default value:** 0.8.1-SNAPSHOT  
* SKIP_TEST  
    **desc:** Skip tests during mvn execution (ex: mvn clean deploy -Dmaven.test.skip=true)  
    **default value:** ON  
* SONAR  
    **desc:** Check code with SonarQube  
    **default value:** OFF  
* GIT_BRANCHNAME  
    **desc:** Git branch for project  
    **default value:** master  
* PROFILE_TYPE  
    **desc:** Profile type  
    **available values:** nexus, jfrog, bintray, sonatype  
* CBUILD  
    **desc:** Development option, it is not recommended to uncheck it. If unchecked it will try to find libnd4j from snapshot repository instead of building it  
    **default value:** ON  




---  

Pipeline downloads code from <http://github.com/deeplearning4j/pipelines.git>, master branch with credentials from jenkins, and run main release/snapshot script `jobs/dl4j/utility_jobs/release.groovy`, which load scripts specific for each platform.  
For example libnd4j groovy scripts:  
1. jobs\dl4j\libnd4j\
\ ----- libnd4j-android-arm.groovy  
\ ----- libnd4j-android-arm.groovy  
\ ----- libnd4j-linux-ppc64le.groovy  
\ ----- libnd4j-linux-x86_64.groovy  
\ ----- libnd4j-macosx-x86_64.groovy  
\ ----- libnd4j-windows-x86_64.groovy  

`jobs\dl4j\libnd4j\build.groovy` - script for each component (arbiter, datavec, libnd4j etc) which can be run as standalone job:  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/Arbiter/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/DataVec/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/libnd4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/deeplearning4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/gym-java-client/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/nd4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/nd4s/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/rl4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/ScalNet/>  


Every job run build.groovy in it own dir:
```
jobs/dl4j/arbiter/build.groovy
jobs/dl4j/datavec/build.groovy
jobs/dl4j/libnd4j/build.groovy
...
```

[release.groovy](/jobs/dl4j/utility_jobs/release.groovy) script starts in parallel N-value of [all-multiplatform](http://master-jenkins.skymind.io:8080/job/dl4j/job/all-multiplatform/) job, N = count of support platform (current value - 6).  
**linux-x86_64** is main platform, RELEASE and SNAPSHOT jobs starts whole build cycle for all 9 components. On nd4s stage task waits until all necessary artifacts will be built for all other platforms (functions.copy_nd4j_native_from_user_content).  
Opens and closes staging repository on jenkins master node.  
All-multiplatform job for all other platforms builds only 2 components (libnd4j, nd4j), collect artifacts and put them to temporary storage  (functions.copy_nd4j_native_to_user_content), to be used later in all-multiplatform job for linux-x86_64 platform.  

[vars.groovy](/jobs/dl4j/vars.groovy) - environment variables for whole build system.  
[functions.groovy](/jobs/dl4j/functions.groovy) - functions which using by build scripts for downloading source code, sonar source code check, pushing tags, upload artifacts to repository, _nd4j_ component download dependencies, download libnd4j pre-built libraries etc.  
[datavec-linux-x86_64.groovy](/jobs/dl4j/datavec/datavec-linux-x86_64.groovy) - builds datavec component for linux x86-64, the main logic of script is:  
`checkout from github-> setup scala (./change-scala-versions.sh) and spark (./change-spark-versions.sh) versions -> maven clean deploy`  
Most of others scripts works same, all artifacts and libraries copying to temp dir for using them in others components build.  


## Build schema  
 <p align="center">
   <img src="/imgs/build_scheme.png"/>
 </p>

_closing stage_ and _github tagging_ not available for **SNAPSHOT**  

If user will push **_Abort_** on **Wait-for-User-Input stage** staging repository left opened and all built artifacts will be uploaded there,
on **_Proceed_** staging repository will be closed and next step pushes tag to github.  

**libnd4j** starts builds of CPU lib, CUDA 7.5 and CUDA 8.0 libs in parallel using docker containers with gcc, g++ and cmake.  
**nd4j** builds on java 1.8 in docker containers which using libs from **libnd4j** build step.  
**nd4s** build through scala builder(sbt) and uses artifacts from nd4j which deployed in temporary user dir. (functions.copy_nd4j_native_to_user_content)  


 <p align="center">
   <img src="/imgs/libnd4j_build_scheme.png"/>
 </p>
