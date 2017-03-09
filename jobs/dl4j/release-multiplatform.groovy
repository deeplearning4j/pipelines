properties([
    [$class: "BuildDiscarderProperty",
        strategy: [
            $class: "LogRotator",
            artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "",
            daysToKeepStr: "",
            numToKeepStr: "10"
        ]
    ],
    [$class: "ParametersDefinitionProperty",
        parameterDefinitions: [
            [$class: "ChoiceParameterDefinition",
                name: "PLATFORM_NAME",
                choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86\nwindows",
                description: "Build project on architecture"
            ],
            [$class: "BooleanParameterDefinition",
                name: "TESTS",
                defaultValue: false,
                description: "Select to run tests during mvn execution"
            ],
            [$class: "BooleanParameterDefinition",
                name: "SONAR",
                defaultValue: false,
                description: "Select to check code with SonarQube"
            ],
            [$class: "StringParameterDefinition",
                name: "GIT_BRANCHNAME",
                defaultValue: "intropro072-01",
                description: "Default Git branch value"
            ],
            [$class: "CredentialsParameterDefinition",
                name: "GITCREDID",
                required: false,
                defaultValue: "github-private-deeplearning4j-id-1",
                description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"
            ],
            [$class: "ChoiceParameterDefinition",
                name: "PROFILE_TYPE",
                choices: "nexus\njfrog\nbintray\nsonatype",
                description: "Profile type"
            ],
            [$class: "BooleanParameterDefinition",
                name: "CBUILD",
                defaultValue: false,
                description: "Select to build libnd4j"
            ],
        ]
    ]
])

env.PDIR = "jobs/dl4j"

node("master") {
    stage("BuildBaseLibs") {
        parallel (
            "Stream 0 x86_64" : {
                build job: 'devel/dl4j/dl4j-release-multiplatform', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-x86_64"]]
            },
            "Stream 1 ppc64le" : {
                build job: 'devel/dl4j/dl4j-release-multiplatform', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-ppc64le"]]
            },
            "Stream 2 android-x86" : {
                build job: 'devel/dl4j/dl4j-release-multiplatform', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-x86"]]
            },
            "Stream 3 android-arm" : {
                build job: 'devel/dl4j/dl4j-release-multiplatform', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-arm"]]
            },
            "Stream 4 android-arm" : {
                build job: 'devel/dl4j/dl4j-release-multiplatform', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "windows"]]
            }
        )
    }
}

    // stage('RELEASE') {
    //
    //   // def isSnapshot = VERSION.endsWith('SNAPSHOT')
    //
    //   if(isSnapshot) {
    //     echo "End of building and publishing of the ${VERSION}"
    //   }
    //   else {
    //     // timeout(time:1, unit:'HOURS') {
    //     timeout(20) {
    //         input message:"Approve release of version ${VERSION} ?"
    //     }
    //
    //     // functions.release("${LIBPROJECT}")
    //     functions.release("${PROJECT}")
    //     functions.release("${DATAVEC_PROJECT}")
    //     functions.release("${DEEPLEARNING4J_PROJECT}")
    //     functions.release("${ARBITER_PROJECT}")
    //     functions.release("${ND4S_PROJECT}")
    //     functions.release("${GYM_JAVA_CLIENT_PROJECT}")
    //     functions.release("${RL4J_PROJECT}")
    //     functions.release("${SCALNET_PROJECT}")
    //
    //   }
    //
    // }

    echo 'MARK: end of release-multiplatform.groovy'
}
