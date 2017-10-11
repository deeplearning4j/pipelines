def notifyFailed() {
  emailext (
      subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
      Check console output at '${env.BUILD_URL}'""",
      to: "${MAIL_RECIPIENT}"
    )
}

env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node("${PLATFORM_NAME}") {
  try {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        properties([
                [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
                [$class: "ParametersDefinitionProperty", parameterDefinitions:
                        [
                                [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.9.2-SNAPSHOT", description: "Deeplearning component release version"],
                                [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Build project on architecture"],
                                [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                                [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "master", description: "Default Git branch value"],
                                [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                                [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\nsonatype\njfrog\nbintray", description: "Profile type"],
                                [$class: "BooleanParameterDefinition", name: "PUSH_LIBND4J_LOCALREPO", defaultValue: false, description: "Select to push libnd4j to choosen staging repo"],
                                [$class: "StringParameterDefinition", name: "BUILD_CUDA_PARAMS", defaultValue: "", description: "Pass build cuda parameters here if you want APPEND default ones (it doesn't apply for CPU builds)\nDefauls:\nlinux: -c cuda -v [8.0]\nmacosx: -c cuda\nwindows: -c cuda -v [8.0]\n"]
                        ]
                ]
        ])

        step([$class: 'WsCleanup'])

        checkout scm

        load "jobs/dl4j/vars.groovy"
        functions = load "${PDIR}/functions.groovy"

        // Remove .git folder from workspace
        functions.rm()

        // Set docker image and parameters for current platform
        functions.def_docker()

        stage("${LIBPROJECT}") {
            load "${PDIR}/${LIBPROJECT}/${LIBPROJECT}-${PLATFORM_NAME}.groovy"
        }

        stage('RELEASE') {

            if (isSnapshot) {
                echo "End of building of the ${LIBPROJECT}-${VERSION}"
            } else {
                // timeout(time:1, unit:'HOURS') {
                timeout(20) {
                    input message: "Approve release of version ${LIBPROJECT}-${VERSION} ?"
                }

                functions.tag("${LIBPROJECT}")
            }

        }

        // step([$class: 'WsCleanup'])

    }
  // send email about successful finishing
  functions.notifySuccessful(currentBuild.displayName)

  } catch (e) {
    currentBuild.result = "FAILED"
    notifyFailed()
    throw e

    }
}
ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j/build.groovy \033[0m"
}
