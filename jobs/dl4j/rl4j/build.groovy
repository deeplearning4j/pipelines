node("${DOCKER_NODE}") {

    properties([
            [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
            [$class: "ParametersDefinitionProperty", parameterDefinitions:
                    [
                            [$class: "StringParameterDefinition", name: "RELEASE_VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                            [$class: "BooleanParameterDefinition", name: "TESTS", defaultValue: false, description: "Select to run tests during mvn execution"],
                            [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                            [$class: "BooleanParameterDefinition", name: "CREATE_TAG", defaultValue: false, description: "Select to create tag for release in git repository"],
//                            [$class: "StringParameterDefinition", name: "ND4J_VERSION", defaultValue: "0.7.2", description: "Path to groovy scripts"],
                            [$class: "StringParameterDefinition", name: "DL4J_VERSION", defaultValue: "0.7.2", description: "Path to groovy scripts"],
//                            [$class: "StringParameterDefinition", name: "DATAVEC_VERSION", defaultValue: "0.7.2", description: "Path to groovy scripts"],
//                            [$class: "ChoiceParameterDefinition", name: "SCALA_VERSION", choices: "2.10\n2.11", description: "Scala version definition"],
//                            [$class: "ChoiceParameterDefinition", name: "CUDA_VERSION", choices: "7.5\n8.0", description: "Cuda version definition"],
                            [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86", description: "OpenBLAS platform-name"],
                            [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                            [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                            [$class: "LabelParameterDefinition", name: "DOCKER_NODE", defaultValue: "jenkins-slave-cuda", description: "Correct parameters:\njenkins-slave-cuda\nsshlocal\npower8\nppc"],
                            [$class: "StringParameterDefinition", name: "PDIR", defaultValue: "jobs/dl4j", description: "Path to groovy scripts"],
                            [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\njfrog\nbintray\nsonatype", description: "Profile type"]
                    ]
            ]
    ])

    step([$class: 'WsCleanup'])

    // dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    functions.rm()

    // Create .m2 direcory
    functions.dirm2()

    // Set docker image and parameters for current platform
    functions.def_docker()

    stage("${RL4J_PROJECT}") {
      load "${PDIR}/${RL4J_PROJECT}/${RL4J_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      // def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${RL4J_PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${RL4J_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${RL4J_PROJECT}")
      }

    }

    // step([$class: 'WsCleanup'])

}
