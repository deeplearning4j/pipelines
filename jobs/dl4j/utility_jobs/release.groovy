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
//                     defaultValue: "0.8.1-SNAPSHOT",
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
env.STAGE_REPO_ID = env.STAGE_REPO_ID ?: ""
node("master") {
    echo "Cleanup WS"
    step([$class: 'WsCleanup'])

    checkout scm

    env.PDIR = "jobs/dl4j"
    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    functions.notifyStarted()

    if (!isSnapshot) {
        functions.cleanup_userContent()
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
                         [$class: 'StringParameterValue', name: 'BUILD_CUDA_PARAMS', value: BUILD_CUDA_PARAMS]
                        ]
            }
        }
        parallel builders
    }


    if (isSnapshot) {

        echo "Snapshots of version ${VERSION} are builded"

    } else {

        stage("Cleanup-User-Content") {
            functions.cleanup_userContent()
        }

        stage("Wait-For-User-Input") {
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
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of release.groovy \033[0m"
    // echo 'MARK: end of release.groovy'
}
