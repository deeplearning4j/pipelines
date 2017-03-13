node("${PLATFORM_NAME}") {

    properties([
            [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
            [$class: "ParametersDefinitionProperty", parameterDefinitions:
                    [
                            [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                            [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86", description: "Build project on architecture"],
//                            [$class: "LabelParameterDefinition", name: "DOCKER_NODE", defaultValue: "jenkins-slave-cuda", description: "Correct parameters:\nFor x86_64-jenkins-slave-cuda,amd64\nfor PowerPC - ppc,power8"],
                            // [$class: "BooleanParameterDefinition", name: "TESTS", defaultValue: false, description: "Select to run tests during mvn execution"],
                            // [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                            [$class: "BooleanParameterDefinition", name: "CREATE_TAG", defaultValue: false, description: "Select to create tag for release in git repository"],
                            // [$class: "StringParameterDefinition", name: "ND4J_VERSION", defaultValue: "", description: "Set preferred nd4j version, leave it empty to use VERSION"],
                            // [$class: "StringParameterDefinition", name: "DL4J_VERSION", defaultValue: "", description: "Set preferred dl4j version, leave it empty to use VERSION"],
                            // [$class: "StringParameterDefinition", name: "DATAVEC_VERSION", defaultValue: "", description: "Set preferred datavec version, leave it empty to use VERSION"],
                            // [$class: "ChoiceParameterDefinition", name: "SCALA_VERSION", choices: "2.10\n2.11", description: "Scala version definition"],
                            // [$class: "ChoiceParameterDefinition", name: "CUDA_VERSION", choices: "7.5\n8.0", description: "Cuda version definition"],
                            [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                            [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                            [$class: "StringParameterDefinition", name: "PDIR", defaultValue: "jobs/dl4j", description: "Path to groovy scripts"],
                            [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\njfrog\nbintray\nsonatype", description: "Profile type"]
                    ]
            ]
    ])

//    step([$class: 'WsCleanup'])

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Set docker image and parameters for current platform
    functions.def_docker()

    stage("${ND4S_PROJECT}") {
      load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      // def isSnapshot = VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${ND4S_PROJECT}-${VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${ND4S_PROJECT}-${VERSION} ?"
        }

        functions.release("${ND4S_PROJECT}")
      }

    }

    // step([$class: 'WsCleanup'])

}
