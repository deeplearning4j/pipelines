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
//             [$class: "BooleanParameterDefinition",
//                 name: "TESTS",
//                 defaultValue: false,
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
    if (str.getClass() == String && str.length()>0) {
        tmpList = []
        for ( i in str.split(",")) {
            def item = i
            tmpList.add(item);
        }
    }
    else {
        error "strToList(): Input arg isn't string or empty, class: ${str.getClass()}, size: ${str.length()}"
    }
    return tmpList
}

node("master") {
    env.PDIR = "jobs/dl4j"
    def platformsList = strToList(PLATFORMS)
    println platformsList
    def builders = [:]
    for (platform in platformsList) {
        println platform
        builders[platform] = {

            node("${node}") {
                build job: "devel/dl4j/all-${platform}", parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "${platform}"]]
                }
            }
        }
    }
    println builders
    parallel builders
}

// node("master") {
//
//     env.PDIR = "jobs/dl4j"
//
//     stage("Build-multiplatform-parallel") {
//         parallel (
//             "Stream 0 linux-x86_64" : {
//                 build job: 'devel/dl4j/all-linux-x86_64', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-x86_64"]]
//             },
//             "Stream 1 linux-ppc64le" : {
//                 build job: 'devel/dl4j/all-linux-ppc64le', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-ppc64le"]]
//             },
//             "Stream 2 android-x86" : {
//                 build job: 'devel/dl4j/all-android-x86', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-x86"]]
//              },
//             "Stream 3 android-arm" : {
//                 build job: 'devel/dl4j/all-android-arm', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-arm"]]
//             },
//             "Stream 4 windows-x86_64" : {
//                 build job: 'devel/dl4j/all-windows', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "windows-x86_64"]]
//             },
//             "Stream 5 macosx-x86_64" : {
//                 build job: 'devel/dl4j/all-macosx', parameters:
//                     [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "macosx"]]
//             }
//         )
//     }
// }
