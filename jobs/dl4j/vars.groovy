// email recipients
env.MAIL_RECIPIENT = "samuel@skymind.io"

isSnapshot = VERSION.endsWith('SNAPSHOT')
env.GpgVAR = VERSION.endsWith('SNAPSHOT') ? "false" : "true"
env.CBUILD = env.CBUILD ?: "true"
env.PUSH_LIBND4J_LOCALREPO = env.PUSH_LIBND4J_LOCALREPO ?: "false"
env.BUILD_CUDA_PARAMS = env.BUILD_CUDA_PARAMS ?: ""
env.PARENT_JOB = env.PARENT_JOB ?: ""

// Utility job names
// env.JOB_RELEASE = "RELEASE"
env.JOB_MULTIPLATFORM = "all-multiplatform"
env.JOB_TAG = "all-tag"

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
env.SKIL_PROJECT = "skil"
env.ND4S_PROJECT = "nd4s"
env.RL4J_PROJECT = "rl4j"
env.SCALNET_PROJECT = "scalnet"
env.DATAVEC_PROJECT = "datavec"
env.PROFILE_TYPE = env.PROFILE_TYPE ?: "nexus"
env.CREATE_RPM = env.CREATE_RPM ?: "false"
env.PUSH_LIBND4J_LOCALREPO = env.PUSH_LIBND4J_LOCALREPO ?: "false"

env.DOCKER_ANDROID_IMAGE = "skymindops/pipelines:android"
env.DOCKER_CENTOS6_CUDA80_AMD64 = "skymindops/pipelines:centos6cuda80"
env.DOCKER_CENTOS6_CUDA90_AMD64 = "skymindops/pipelines:centos6cuda90"
env.DOCKER_CENTOS7_CUDA80_AMD64 = "huitseeker/dl4j-centos7-dev-env:latest"
env.DOCKER_UBUNTU14_CUDA80_AMD64 = "skymindops/pipelines:ubuntu14cuda80"
env.DOCKER_UBUNTU16_CUDA90_AMD64 = "skymindops/pipelines:ubuntu16cuda90"
env.DOCKER_UBUNTU16_CUDA80_PPC = "skymindops/pipelines:ubuntu16cuda80-ppc64le"
env.DOCKER_UBUNTU16_CUDA90_PPC = "skymindops/pipelines:ubuntu16cuda90-ppc64le"

env.JENKINS_DOCKER_M2DIR = "/srv/jenkins/storage/docker_m2"
env.JENKINS_DOCKER_SBTDIR = "/srv/jenkins/storage/docker_ivy2"

// env.JENKINS_M2DIR_PPC64LE = "/srv/jenkins/storage/docker_m2"
// env.JENKINS_SBTDIR_PPC64LE = "/srv/jenkins/storage/docker_ivy2"

// Jenkins seem to be mounting -v ${WORKSPACE}:${WORKSPACE}:z on its own, see
// https://issues.jenkins-ci.org/browse/JENKINS-29239?focusedCommentId=285440&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-285440
dockerParams_init = "-v ${JENKINS_DOCKER_M2DIR}/${PROFILE_TYPE}:/home/jenkins/.m2:z"
dockerParams_nvidia = "-v ${JENKINS_DOCKER_M2DIR}/${PROFILE_TYPE}:/home/jenkins/.m2:z -v ${JENKINS_DOCKER_SBTDIR}:/home/jenkins/.ivy2:z --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0"
dockerParams_tmpfs_nvidia = "-v ${JENKINS_DOCKER_M2DIR}/${PROFILE_TYPE}:/home/jenkins/.m2:z -v ${JENKINS_DOCKER_SBTDIR}:/home/jenkins/.ivy2:z --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --tmpfs /tmp:size=8g"

// env.SBTCREDID = "sbt-local-artifactory-id-1"
// env.SBTCREDID = "SBT_CREDENTIALS_DO-192"

/** Below variables need to be reviewed once release approach will be approved
 */

/*
SONAR_SERVER should be configured in Jenkins/configure in SonarQube servers
SONAR_SCANNER should be configured in Jenkins/configureTools in SonarQube Scanner
*/
env.SONAR_SERVER = "SonarQubeServer"
// env.SONAR_SCANNER = "SS28"
// env.SONAR_SCANNER = "SS29"
env.SONAR_SCANNER = "SS30"
// Test folder path
env.TEST_CPU_FOLDER = 'tests_cpu'
