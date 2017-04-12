// def notifyFailed() {
//   emailext (
//       subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
//       body: """FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
//       Check console output at '${env.BUILD_URL}'""",
//       to: "samuel@skymind.io"
//     )
// }

env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node("${PLATFORM_NAME}") {
  try {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        properties([
                [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
                [$class: "ParametersDefinitionProperty", parameterDefinitions:
                        [
                                [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.8.1-SNAPSHOT", description: "Deeplearning component release version"],
                                [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64", description: "Build project on architecture"],
                                [$class: "StringParameterDefinition", name: "STAGE_REPO_ID", defaultValue: "", description: "Staging repository Id"],
                                [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "master", description: "Default Git branch value"],
                                [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                                [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "sonatype\nnexus\njfrog\nbintray", description: "Profile type"],
                                [$class: "StringParameterDefinition", name: "PARENT_JOB", defaultValue: "SNAPSHOT-34", description: "This parameter sets by upstream job (release or snapshot).\n!!! Do not set it manually !!!"]
                        ]
                ]
        ])

        checkout scm

        load "jobs/dl4j/vars.groovy"
        functions = load "${PDIR}/functions.groovy"

        // Remove .git folder from workspace
        functions.rm()

        // Set docker image and parameters for current platform
        functions.def_docker()

        stage("${ND4S_PROJECT}") {
            load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-${PLATFORM_NAME}.groovy"
        }

        stage('RELEASE') {

            if (isSnapshot) {
                echo "End of building and publishing of the ${ND4S_PROJECT}-${VERSION}"
            } else {
                // timeout(time:1, unit:'HOURS') {
                timeout(20) {
                    input message: "Approve release of version ${ND4S_PROJECT}-${VERSION} ?"
                }
                functions.tag("${ND4S_PROJECT}")
            }

        }

       // step([$class: 'WsCleanup'])
    }
  // send email about successful finishing
  // functions.notifySuccessful(currentBuild.displayName)

  } catch (e) {
    currentBuild.result = "FAILED"
    // notifyFailed()
    throw e

    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of nd4s/build.groovy \033[0m"
}
