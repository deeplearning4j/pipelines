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
//                     defaultValue: "0.7.3-SNAPSHOT",
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
//                 defaultValue: "intropro072-01",
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
//                 choices: "nexus\njfrog\nbintray\nsonatype",
//                 description: "Profile type"
//             ],
//             [$class: "BooleanParameterDefinition",
//                 name: "CBUILD",
//                 defaultValue: true,
//                 description: "Select to build libnd4j"
//             ],
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

    if (!isSnapshot) {
        functions.cleanup_userContent()
        functions.open_staging_repository("${PROFILE_TYPE}")
    }

    stage("RunningBuilds") {
        def platformsList = strToList(PLATFORMS)
        def builders = [:]
        for (platform in platformsList) {
            def xplatform = platform
            builders[platform] = {
                build job: "./all-multiplatform", parameters:
                        [[$class: 'StringParameterValue', name: 'PLATFORM_NAME', value: xplatform],
                         [$class: 'StringParameterValue', name: 'VERSION', value: VERSION],
                         [$class: 'BooleanParameterValue', name: 'SKIP_TEST', value: SKIP_TEST.toBoolean()],
                         [$class: 'BooleanParameterValue', name: 'SONAR', value: SONAR.toBoolean()],
                         [$class: 'StringParameterValue', name: 'GIT_BRANCHNAME', value: GIT_BRANCHNAME],
                         [$class: 'StringParameterValue', name: 'GITCREDID', value: GITCREDID],
                         [$class: 'StringParameterValue', name: 'PROFILE_TYPE', value: PROFILE_TYPE],
                         [$class: 'BooleanParameterValue', name: 'CBUILD', value: CBUILD.toBoolean()],
                         [$class: 'StringParameterValue', name: 'STAGE_REPO_ID', value: STAGE_REPO_ID, default: ""]
                        ]
            }
        }
        parallel builders
    }


    if (!isSnapshot) {
      stage("Cleanup-User-Content") {
        functions.cleanup_userContent()
      }

      stage("Close-Staging-Repository") {
        functions.close_staging_repository("${PROFILE_TYPE}")
      }

      stage("Clone-Progects-Repositories") {
        node("linux-x86_64") {
          functions.get_project_code("${PROJECT}")
          functions.get_project_code("${LIBPROJECT}")
          functions.get_project_code("${DATAVEC_PROJECT}")
          functions.get_project_code("${DEEPLEARNING4J_PROJECT}")
          functions.get_project_code("${ARBITER_PROJECT}")
          functions.get_project_code("${ND4S_PROJECT}")
          functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
          functions.get_project_code("${RL4J_PROJECT}")
          functions.get_project_code("${SCALNET_PROJECT}")
        }
      }

      stage("Clone-Progects-Repositories") {
        node("linux-x86_64") {
          functions.tag("${PROJECT}")
          functions.tag("${LIBPROJECT}")
          functions.tag("${DATAVEC_PROJECT}")
          functions.tag("${DEEPLEARNING4J_PROJECT}")
          functions.tag("${ARBITER_PROJECT}")
          functions.tag("${ND4S_PROJECT}")
          functions.tag("${GYM_JAVA_CLIENT_PROJECT}")
          functions.tag("${RL4J_PROJECT}")
          functions.tag("${SCALNET_PROJECT}")
        }
      }

    }
}
