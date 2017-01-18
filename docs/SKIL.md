Notices for release engineers originally located at

https://github.com/SkymindIO/SKIL/wiki/Release-Notes

----
Here are miscellaneous issues that I noted when doing the release for version 0.7.2, as well as recommendations for future releases.

Checklist Before Release
------------------------
We should make sure to check that at least the following works before making a release.

Examples:
* At least LenetMnist, GravesLSTM, MLPClassifier*, XorExample
* Each combination of: Windows/Linux/OSX and Native (with/without MKL), CUDA, CUDA+CUDNN
* Plus: spark local examples (at least MnistMLPExample)

Unit tests:
* ND4J
* DL4J (on both native + CUDA)
    - Including: DL4J Spark unit tests: native, CUDA, cuDNN
* DataVec
* Arbiter


CUDA
----
CUDA builds currently take a lot of time (about 2 hours per platform), so not fast enough to make them part of the build process of nd4j. If we could manage to make it build within a minute or so, it would make sense to have libnd4j part of the Maven lifecycle of nd4j, simplifying the whole build process.


Maven
-----
Ideally, for pure Java projects, we would only need to run 2 commands:
```
mvn release:prepare
mvn release:perform
```

But support for platform-specific artifacts is not so simple. I've updated the pom.xml files of nd4j, datavec, deeplearning4j, and arbiter with the `nexus-staging-maven-plugin` to keep the staging repository open on https://oss.sonatype.org/ . This allows us to run the build on multiple platforms (minus stuff that we can disable easily like Javadoc) and accumulate the native binaries all in one place. When `nexus-staging-maven-plugin` opens a repository, it returns a repositoryId like `orgnd4j-xxxx` that we need to note down. When starting the build on other platforms, we need to specify `-DstagingRepositoryId=orgnd4j-xxxx` on the command line to have it deploy the artifacts at the right place. (With the release plugin, that command line option needs to be `-Darguments=-DstagingRepositoryId=orgnd4j-xxxx`.) However, I prepared some script files to automate most of that -- without using the release plugin.

Now, Maven will want to sign artifacts with GPG and connect to the Nexus server from each platform, so this means we have to copy our credentials into the `~/.gnupg` and `~/.m2` directories on all platforms, which isn't very practical. Maybe some sort of remotely mounted home directory could solve that? For now, they are all copied manually, and below are actual commands used to build for the release on all platforms.

#### libnd4j and nd4j
On Fedora:
```
bash perform-release.sh 0.7.2 0.7.3-SNAPSHOT
# Note down the staging repositoryId created for us...
```

On other platforms (CentOS, Ubuntu for POWER8, OS X, Windows, etc):
```
bash perform-release.sh 0.7.2 0.7.3-SNAPSHOT orgnd4j-xxxx
```

#### Others (datavec, deeplearning4j, arbiter, nd4s, gym-java-client, rl4j, scalnet, etc)
```
bash perform-release.sh 0.7.2 0.7.3-SNAPSHOT orgnd4j-xxxx
```


Linux
-----
I created the build for 0.7.2 in a Docker container of centos:6 with maven30 and devtoolset-3 from scl, and it mostly ran alright, but strangely enough Javadoc didn't work well and failed the build. It works on Fedora 23, not too well, but it works. Still, since we are supposed to support the enterprise, we should make sure that things build fully on older software like CentOS 6 and Ubuntu 14.04.

Command used to build the container:
```
sudo docker run -it centos:6 /bin/bash
```

Commands used to install stuff inside the container:
```
yum install centos-release-scl-rh epel-release
yum install devtoolset-3-toolchain maven30 cmake3 git
scl enable devtoolset-3 maven30 bash
```
We should ideally create a small Dockerfile for that...


OS X
----
OS X comes with the Accelerate framework that contains a version of BLAS and LAPACK, but it is buggy, so we still need to rely on OpenBLAS, or MKL when it is installed.

Libraries on that platform are harder to bundle than on Linux or Windows. For example, `rpath` is not honored, unless specified in the library we linked to. For OpenMP and OpenBLAS, we can work around that by running the following commands on the files from the GCC toolchain as used by ND4J and required for OpenBLAS installed via `brew install gcc5` packages of Homebrew -- before starting the build:

```bash
install_name_tool -add_rpath /usr/local/opt/gcc5/lib/gcc/5/ -add_rpath @loader_path/. -id @rpath/libgomp.1.dylib libgomp.1.dylib
install_name_tool -add_rpath /usr/local/opt/gcc5/lib/gcc/5/ -add_rpath @loader_path/. -id @rpath/libstdc++.6.dylib libstdc++.6.dylib
install_name_tool -add_rpath /usr/local/opt/gcc5/lib/gcc/5/ -add_rpath @loader_path/. -id @rpath/libgfortran.3.dylib libgfortran.3.dylib
install_name_tool -add_rpath /usr/local/opt/gcc5/lib/gcc/5/ -add_rpath @loader_path/. -id @rpath/libquadmath.0.dylib libquadmath.0.dylib
install_name_tool -add_rpath /usr/local/opt/gcc5/lib/gcc/5/ -add_rpath @loader_path/. -id @rpath/libgcc_s.1.dylib libgcc_s.1.dylib
install_name_tool -change /usr/local/Cellar/gcc5/5.4.0/lib/gcc/5/libquadmath.0.dylib @rpath/libquadmath.0.dylib libgfortran.3.dylib
install_name_tool -change /usr/local/lib/gcc/5/libgcc_s.1.dylib  @rpath/libgcc_s.1.dylib libgomp.1.dylib
install_name_tool -change /usr/local/lib/gcc/5/libgcc_s.1.dylib  @rpath/libgcc_s.1.dylib libstdc++.6.dylib
install_name_tool -change /usr/local/lib/gcc/5/libgcc_s.1.dylib  @rpath/libgcc_s.1.dylib libgfortran.3.dylib
install_name_tool -change /usr/local/lib/gcc/5/libgcc_s.1.dylib  @rpath/libgcc_s.1.dylib libquadmath.0.dylib
```

Whether we can do the same with MKL remains to be seen. But NVIDIA has apparently thought this through for CUDA though: http://docs.nvidia.com/cuda/cuda-c-best-practices-guide/index.html#redistribution--which-files


Windows
-------
The `maven-release-plugin` is totally broken on that platform. Although we do not use it anymore, if one wishes to use it again, please read the following. It tries to use backslashes in paths on the git command while git only recognizes slashes, tries to use gpg-agent when MSYS2 is not actually capable, and Javadoc fails. So, the idea is to run the prepare stage, skipping Javadoc, and it will bail out when everything is done, when trying to run `git commit`:
```
$ mvn release:prepare -Darguments=-Dmaven.javadoc.skip
```

At that point, we can deploy manually only the binaries we are interested in, preferably directly inside the `nd4j-backends` directory:
```
$ mvn deploy -Dgpg.useagent=false -DperformRelease -Psonatype-oss-release -DskipTests -Dmaven.javadoc.skip -DstagingRepositoryId=orgnd4j-xxxx
```
But it's easier to just redeploy everything as shown in the Maven section above.


Power
-----
Various issues have been fixed and everything builds pretty much out of the box with Ubuntu 16.04, however we only have access to machines from [SuperVessel](http://www.ptopenlab.com/) to do our builds and that comes with Ubuntu 14.04, so 0.7.2 was released with binaries for that platform. Note that SuperVessel offers machines with only 2 GB of RAM, so it takes more than a few hours to build libnd4j for CUDA.

Commands used to install additional stuff inside an instance of "Open Road QuickStart":
```
sudo add-apt-repository ppa:ubuntu-toolchain-r/test
sudo apt-get update
sudo apt-get install g++-4.9 gfortran-4.9 libblas-dev liblapack-dev maven openjdk-7-jdk pkg-config alsa-lib-devel libxfixes-dev libusb-1.0-0-dev libraw1394-dev
sudo update-alternatives --install /usr/bin/gcc gcc /usr/bin/gcc-4.9 60 --slave /usr/bin/g++ g++ /usr/bin/g++-4.9 --slave /usr/bin/gfortran gfortran /usr/bin/gfortran-4.9
sudo update-alternatives --install /usr/bin/java java /opt/ibm/ibm-sdk-lop/ibm-java-80/bin/java 2000
sudo update-alternatives --install /usr/bin/javac javac /opt/ibm/ibm-sdk-lop/ibm-java-80/bin/javac 2000
export _JAVA_OPTIONS=-Xmx4g
```

We also now have access to a better machine (8 CPU, 16G RAM, 160G Disk) from the [OSU Open Sourse Lab](http://osuosl.org/services/powerdev/). We can connect with SSH via centos@140.211.168.159. (Please contact Samuel to have your public key added.) On that machine, we can also get an instance of Ubuntu 14.04 with Docker:
```
sudo docker run -it ppc64le/ubuntu:14.04 /bin/bash
```
