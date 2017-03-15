isSnapshot = VERSION.endsWith('SNAPSHOT')
env.GpgVAR = VERSION.endsWith('SNAPSHOT') ? "false" : "true"


settings_xml = 'maven-settings-id-1'
// gitcredid = 'github-private-deeplearning4j-id-1'
// env.GITCREDID = "github-private-deeplearning4j-id-1"
env.PDIR = "jobs/dl4j"
env.ACCOUNT = "deeplearning4j"
env.PROJECT = "nd4j"
env.LIBPROJECT = "libnd4j"
env.ARBITER_PROJECT = "arbiter"
env.DEEPLEARNING4J_PROJECT = "deeplearning4j"
env.GYM_JAVA_CLIENT_PROJECT = "gym-java-client"
env.ND4S_PROJECT = "nd4s"
env.RL4J_PROJECT = "rl4j"
env.SCALNET_PROJECT = "scalnet"
env.DATAVEC_PROJECT = "datavec"

env.DOCKER_UBUNTU14_CUDA75_AMD64 = "deeplearning4j-docker-registry.bintray.io/ubuntu14cuda75:latest"
env.DOCKER_UBUNTU14_CUDA80_AMD64 = "deeplearning4j-docker-registry.bintray.io/ubuntu14cuda80:latest"
env.DOCKER_CENTOS6_CUDA75_AMD64 = "deeplearning4j-docker-registry.bintray.io/centos6cuda75:latest"
env.DOCKER_CENTOS6_CUDA80_AMD64 = "deeplearning4j-docker-registry.bintray.io/centos6cuda80:latest"
env.DOCKER_CUDA_PPC = "deeplearning4j-docker-registry.bintray.io/ubuntu14-ppc64le:latest"
env.DOCKER_ANDROID_IMAGE = "deeplearning4j-docker-registry.bintray.io/android:latest"
env.JENKINS_M2DIR_AMD64 = "/srv/jenkins/storage/docker_m2"
env.JENKINS_SBTDIR_AMD64 = "/srv/jenkins/storage/docker_ivy2"
env.JENKINS_M2DIR_PPC64LE = "/srv/jenkins/storage/docker_m2"
env.JENKINS_SBTDIR_PPC64LE = "/srv/jenkins/storage/docker_ivy2"


dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw"
dockerParams_tmpfs = "-v ${WORKSPACE}:${WORKSPACE}:rw ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw --tmpfs /tmp:size=3g --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
dockerParams_ppc = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_PPC64LE}:/home/jenkins/.m2:rw"
dockerParams_nvidia = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw  -v ${JENKINS_SBTDIR_AMD64}:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

env.NEXUS_LOCAL = "http://master-jenkins.eastus.cloudapp.azure.com:8088"
// env.SBTCREDID = "sbt-local-artifactory-id-1"
// env.SBTCREDID = "SBT_CREDENTIALS_DO-192"

/** Below variables need to be reviewed once release approach will be approved
 */
// env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "0.7.2-SNAPSHOT"
env.PROFILE_TYPE = env.PROFILE_TYPE ?: "jfrog"
env.ND4J_VERSION = env.ND4J_VERSION ?: "${VERSION}"
env.DL4J_VERSION = env.DL4J_VERSION ?: "${VERSION}"
env.DATAVEC_VERSION = env.DATAVEC_VERSION ?: "${VERSION}"
