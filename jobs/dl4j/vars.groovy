isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')
// settings_xml = 'maven-settings-id-1'
// dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
settings_xml = 'maven-settings-id-2'
dockerParams_tmpfs = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --tmpfs /tmp:size=3g --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
dockerParams_ppc = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw -v /var/lib/jenkins/tools/docker_ivy2:/home/jenkins/.ivy2:rw -v /mnt/libnd4j:/libnd4j"
dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v /var/lib/jenkins/tools/docker_m2:/home/jenkins/.m2:rw  -v /var/lib/jenkins/tools/docker_ivy2:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
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
env.DOCKER_CUDA_PPC = "ubuntu14ppc:latest"
// env.SBTCREDID = "sbt-local-artifactory-id-1"
// env.SBTCREDID = "SBT_CREDENTIALS_DO-192"

/** Below variables need to be reviewed once release approach will be approved
 */
env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "0.7.2-SNAPSHOT"
env.PROFILE_TYPE = env.PROFILE_TYPE ?: "jfrog"
env.ND4J_VERSION = env.ND4J_VERSION ?: "0.7.2"
env.DL4J_VERSION = env.DL4J_VERSION ?: "0.7.2"
env.DATAVEC_VERSION = env.DATAVEC_VERSION ?: "0.7.2"
