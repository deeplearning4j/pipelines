# Jenkins pipeline for deeplearning4j components
## Build schema  
 <p align="center">
   <img src="/imgs/build_scheme.png"/>
 </p>

 _closing stage_ and _github tagging_ not available for **SNAPSHOT**  

 If user selects **_Abort_** on **Wait-for-User-Input stage**
 - staging repository stays opened and collects all built artifacts,
 on **_Proceed_**
 - staging repository will be closed and next step pushes tags to github.  

 **libnd4j** starts builds of CPU lib, CUDA 7.5 and CUDA 8.0 libs in parallel using docker containers with gcc, g++ and cmake.  
 **nd4j** builds on java 1.8 in docker containers using libs from **libnd4j** build step.  
 **nd4s** build through scala builder(sbt) and uses artifacts from nd4j which are copied to temporary dir in Jenkins userContent. (functions.copy_nd4j_native_to_user_content)  

  <p align="center">
    <img src="/imgs/libnd4j_build_scheme.png"/>
  </p>
 
## **RELEASE**  
Launches all-multiplatform jobs in parallel for all platforms. Uploads all libraries to the staging repository and pushes tags to github.  
Link: http://master-jenkins.skymind.io:8080/job/dl4j/job/RELEASE  
Job parameters:  
* PLATFORMS:  
    **desc:** Build libraries on one or several platforms in parallel, linux-x86_64 and android* platforms can be built on the same jenkins linux node, windows-x86_64, linux-ppc64le and macosx-x86_64 should be built on their native jenkins nodes (build makes a choice of corresponding node automatically by the labels). If any platform unchecked - RELEASE won't be successful because of nd4s dependency of all platforms. If you need to release without one or more platforms - scripts should be edited.  
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
    **desc:** Development option, it is not recommended to uncheck it. If unchecked - skips libnd4j building.  
    **default value:** ON  
* TAG  
    **desc:** push tags to GitHub  
    **default value:** OFF  

## **SNAPSHOT**  
Start all-multiplatform jobs in parallel to build components for all platforms.  
Basically it is same to RELEASE excluding upload to snapshot repository and git tagging.  
link: http://master-jenkins.skymind.io:8080/job/dl4j/job/SNAPSHOT/
Job parameters:  
* PLATFORMS:  
    **desc:** Build libraries for one or several platforms in parallel, linux-x86_64 and android* platforms can be built on same linux node, windows-x86_64, linux-ppc64le and macosx-x86_64 should be built on their native jenkins nodes (build makes a choice of corresponding node automatically by the labels). If any platform unchecked - SNAPSHOT won't be successful because of nd4s dependency  of all platforms. If you need to create snapshots without one or more platforms - scripts should be edited.
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

Pipeline downloads code from master branch of <http://github.com/deeplearning4j/pipelines.git>,
by credentials (github-private-deeplearning4j-id-1) configured in  Jenkins: (http://master-jenkins.skymind.io:8080/credentials/store/system/domain/_/credential/github-private-deeplearning4j-id-1/),   
and run main release/snapshot script `jobs/dl4j/utility_jobs/release.groovy`, which loads specific scripts for each platform. Two components - **libnd4j** and **nd4j** have to be built on all platforms, others are to be built on linux-x86_64 only.   

##### Scripts for builds:
1. **libnd4j** scripts in [jobs/dl4j/libnd4j/](/jobs/dl4j/libnd4j/):  
[libnd4j-android-arm.groovy](/jobs/dl4j/libnd4j/libnd4j-android-arm.groovy)  
[libnd4j-android-x86.groovy](/jobs/dl4j/libnd4j/libnd4j-android-x86.groovy)  
[libnd4j-linux-ppc64le.groovy](/jobs/dl4j/libnd4j/libnd4j-linux-ppc64le.groovy)    
[libnd4j-linux-x86_64.groovy](/jobs/dl4j/libnd4j/libnd4j-linux-x86_64.groovy)  
[libnd4j-macosx-x86_64.groovy](/jobs/dl4j/libnd4j/libnd4j-macosx-x86_64.groovy)
[libnd4j/libnd4j-windows-x86_64.groovy](/jobs/dl4j/libnd4j/libnd4j-windows-x86_64.groovy)  

2. **nd4j** scripts in [jobs/dl4j/nd4j/](/jobs/dl4j/nd4j):    
[nd4j-android-arm.groovy](/jobs/dl4j/nd4j/nd4j-android-arm.groovy)  
[nd4j-android-x86.groovy](/jobs/dl4j/nd4j/nd4j-android-x86.groovy)   
[nd4j-linux-ppc64le.groovy](/jobs/dl4j/nd4j/nd4j-linux-ppc64le.groovy)  
[nd4j-linux-x86_64.groovy](/jobs/dl4j/nd4j/nd4j-linux-x86_64.groovy)  
[nd4j-macosx-x86_64.groovy](/jobs/dl4j/nd4j/nd4j-macosx-x86_64.groovy)  
[nd4j-windows-x86_64.groovy](/jobs/dl4j/nd4j/nd4j-windows-x86_64.groovy)  

3. **datavec** script:  
[jobs/dl4j/datavec/datavec-linux-x86_64.groovy](/jobs/dl4j/datavec/datavec-linux-x86_64.groovy)

4. **deeplearning4j** script:  
[jobs/dl4j/deeplearning4j/deeplearning4j-linux-x86_64.groovy](/jobs/dl4j/deeplearning4j/deeplearning4j-linux-x86_64.groovy)

5. **arbiter** script:  
[jobs/dl4j/arbiter/arbiter-linux-x86_64.groovy](/jobs/dl4j/arbiter/arbiter-linux-x86_64.groovy)

6. **nd4s** script:  
[jobs/dl4j/nd4s/nd4s-linux-x86_64.groovy](/jobs/dl4j/nd4s/nd4s-linux-x86_64.groovy)

7. **gym-java-client** script:  
[jobs/dl4j/gym-java-client/gym-java-client-linux-x86_64.groovy](/jobs/dl4j/gym-java-client/gym-java-client-linux-x86_64.groovy)

8. **rl4j** script:  
[jobs/dl4j/rl4j/rl4j-linux-x86_64.groovy](/jobs/dl4j/rl4j/rl4j-linux-x86_64.groovy)

9. **scalnet** groovy scripts:  
[jobs/dl4j/scalnet/scalnet-linux-x86_64.groovy](/jobs/dl4j/scalnet/scalnet-linux-x86_64.groovy)

`jobs/dl4j/*/build.groovy` - script for each component (arbiter, datavec, libnd4j etc) which can be run as standalone job:  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/Arbiter/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/DataVec/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/libnd4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/deeplearning4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/gym-java-client/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/nd4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/nd4s/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/rl4j/>  
<http://master-jenkins.skymind.io:8080/job/dl4j/job/ScalNet/>  


Every job run build.groovy from corresponding folder:
```
jobs/dl4j/arbiter/build.groovy
jobs/dl4j/datavec/build.groovy
jobs/dl4j/libnd4j/build.groovy
...
```

[release.groovy](/jobs/dl4j/utility_jobs/release.groovy) the script starts N-value of [all-multiplatform](http://master-jenkins.skymind.io:8080/job/dl4j/job/all-multiplatform/) jobs in parallel, N = count of support platform (current value - 6).  
**linux-x86_64** is the main platform, RELEASE and SNAPSHOT jobs starts whole build cycle for all 9 components. On nd4s stage task waits until all necessary artifacts are built for all other platforms (functions.copy_nd4j_native_from_user_content).  
Opens and closes staging repository on jenkins master node.  
All-multiplatform job for all other platforms builds only 2 components (libnd4j, nd4j), collect artifacts and put them to temporary storage  (functions.copy_nd4j_native_to_user_content), to be used later in all-multiplatform job for linux-x86_64 platform.  

[vars.groovy](/jobs/dl4j/vars.groovy) - environment variables for whole build system.  
[functions.groovy](/jobs/dl4j/functions.groovy) - functions used by build scripts for downloading source code, code check with sonar, pushing tags, upload artifacts to repository etc.  

### ON FAIL
 If any platform will fail you may try to relaunch all-multiplatform job choosing the corresponding platform (the one which has failed)  
 first - you need to notice the **stagingRepositoryId** in the log of the failed RELEASE job:  
 <p align="center">
   <img src="/imgs/repo_id.png"/>
 </p>
 Than launch all-multiplatform job passing noticed **stagingRepositoryId** right **VERSION**, **PROFILE_TYPE** and **PARENT_JOB** - they should be the same as for failed RELEASE job, e.g:  
 <p align="center">
   <img src="/imgs/macosx_04.png"/>
 </p> 
 **PARENT_JOB** parameter based on job type and build number
 <p align="center">
   <img src="/imgs/job_type_build.png"/>
 </p> 
 The RELEASE job with one or more failed platform builds should wait (at the **nd4s-Platform-Builds-Wait** stage ) for relaunched builds successful finishing.  
 (_that doesn’t work in case of linux-x86_64 build failing, ‘cos that is the main stream and if it fails - the whole RELEASE job should be restarted from scratch_)  
 _No such function for SNAPSHOT_
