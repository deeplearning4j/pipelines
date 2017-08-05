// properties([
//     [$class: "BuildDiscarderProperty",
//         strategy: [
//             $class: "LogRotator",
//             artifactDaysToKeepStr: "",
//             artifactNumToKeepStr: "",
//             daysToKeepStr: "",
//             numToKeepStr: "10"
//         ]
//     ],
//     [$class: "ParametersDefinitionProperty",
//         parameterDefinitions: [
//                 [$class: "StringParameterDefinition",
//                     name: "VERSION",
//                     defaultValue: "0.9.1-SNAPSHOT",
//                     description: "Deeplearning component release version"
//                 ],
//             [$class: "BooleanParameterDefinition",
//                 name: "SKIP_TEST",
//                 defaultValue: true,
//                 description: "Select to run tests during mvn execution"
//             ],
//             [$class: "BooleanParameterDefinition",
//                 name: "SONAR",
//                 defaultValue: false,
//                 description: "Select to check code with SonarQube"
//             ],
//             [$class: "StringParameterDefinition",
//                 name: "GIT_BRANCHNAME",
//                 defaultValue: "master",
//                 description: "Default Git branch value"
//             ],
//             [$class: "CredentialsParameterDefinition",
//                 name: "GITCREDID",
//                 required: false,
//                 defaultValue: "github-private-deeplearning4j-id-1",
//                 description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"
//             ],
//             [$class: "ChoiceParameterDefinition",
//                 name: "PROFILE_TYPE",
//                 choices: "sonatype\nnexus\njfrog\nbintray",
//                 description: "Profile type"
//             ],
//             [$class: "BooleanParameterDefinition",
//                 name: "CBUILD",
//                 defaultValue: true,
//                 description: "Select to build libnd4j"
//             ],
                // [$class: "StringParameterDefinition",
                //     name: "BUILD_CUDA_PARAMS",
                //     defaultValue: "",
                //     description: "Append default parameters for buildnativeoperations.sh, defaults:\nlinux: -c cuda -v 7.5; -c cuda -v 8.0\nwindows: -c cuda -v 7.5; -c cuda -v 8.0\nmacosx: -c cuda\nIt doesn't apply for CPU builds!!!"
                // ]
//         ]
//     ]
// ])


def strToList(str) {
    if (str.getClass() == String && str.length() > 0) {
        tmpList = []
        for (i in str.split(",")) {
            def item = i
            tmpList.add(item);
        }
    } else {
        error "strToList(): Input arg isn't string or empty, class: ${str.getClass()}, size: ${str.length()}"
    }
    return tmpList
}

def notifyFailed() {
  emailext (
      subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
      body: """FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':
      Check console output at '${env.BUILD_URL}'""",
      to: "${MAIL_RECIPIENT}"
    )
}

env.STAGE_REPO_ID = env.STAGE_REPO_ID ?: ""
node("master") {
  try {
    // currentBuild.displayName = "#${currentBuild.number}"
    echo "Cleanup WS"
    step([$class: 'WsCleanup'])

    checkout scm

    env.PDIR = "jobs/dl4j"
    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // send email about starting
    functions.notifyStarted(currentBuild.displayName)

    if (!isSnapshot) {
        functions.open_staging_repository("${PROFILE_TYPE}")
        // functions.notifyRepositoryStatus('opened')
    }

    stage("RunningBuilds") {
        def platformsList = strToList(PLATFORMS)
        def builders = [:]
        for (platform in platformsList) {
            def xplatform = platform
            builders[platform] = {
                build job: "./${JOB_MULTIPLATFORM}", parameters:
                        [[$class: 'StringParameterValue', name: 'PLATFORM_NAME', value: xplatform],
                         [$class: 'StringParameterValue', name: 'VERSION', value: VERSION],
                         [$class: 'BooleanParameterValue', name: 'SKIP_TEST', value: SKIP_TEST.toBoolean()],
                         [$class: 'BooleanParameterValue', name: 'SONAR', value: SONAR.toBoolean()],
                         [$class: 'StringParameterValue', name: 'GIT_BRANCHNAME', value: GIT_BRANCHNAME],
                         [$class: 'StringParameterValue', name: 'GITCREDID', value: GITCREDID],
                         [$class: 'StringParameterValue', name: 'PROFILE_TYPE', value: PROFILE_TYPE],
                         [$class: 'BooleanParameterValue', name: 'CBUILD', value: CBUILD.toBoolean()],
                         [$class: 'StringParameterValue', name: 'STAGE_REPO_ID', value: STAGE_REPO_ID, default: ""],
                         [$class: 'StringParameterValue', name: 'BUILD_CUDA_PARAMS', value: BUILD_CUDA_PARAMS],
                         [$class: 'StringParameterValue', name: 'PARENT_JOB', value: JOB_BASE_NAME + "-" + BUILD_ID]
                        ]
            }
        }
        parallel builders
    }

    stage("Cleanup-User-Content") {
        functions.cleanup_userContent()
    }

    if (isSnapshot) {

        echo "Snapshots of version ${VERSION} are built"

    } else {

        stage("Wait-For-User-Input") {

            functions.notifyInput(currentBuild.displayName)

            timeout(time: 77, unit: 'DAYS') {
                input message:"Approve release of version ${VERSION} ?"
            }
        }

        stage("Close-Staging-Repository") {
            functions.close_staging_repository("${PROFILE_TYPE}")
        }

        stage("Tag-Release") {

            build job: "./${JOB_TAG}", parameters:
                [[$class: 'StringParameterValue', name: 'VERSION', value: VERSION],
                 [$class: 'StringParameterValue', name: 'GIT_BRANCHNAME', value: GIT_BRANCHNAME],
                 [$class: 'BooleanParameterValue', name: 'TAG', value: TAG.toBoolean()],
                 [$class: 'StringParameterValue', name: 'GITCREDID', value: GITCREDID]
                ]

        }
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
    echo "\033[42m MARK: end of release.groovy \033[0m"
    // echo 'MARK: end of release.groovy'
}
