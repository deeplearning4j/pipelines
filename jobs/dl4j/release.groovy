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


node("master") {

    env.PDIR = "jobs/dl4j"

    stage("Build-multiplatform-parallel") {
        parallel (
            "Stream 0 linux-x86_64" : {
                build job: 'devel/dl4j/all-deeplearning4j-linux-x86_64', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-x86_64"],
                    [$class: 'BooleanParameterDefinition', name:'CBUILD', value: "${CBUILD}"]]
            },
            "Stream 1 linux-ppc64le" : {
                build job: 'devel/dl4j/all-deeplearning4j-linux-ppc64le', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-ppc64le"],
                    [$class: 'BooleanParameterDefinition', name:'CBUILD', value: "${CBUILD}"]]
            },
            "Stream 2 android-x86" : {
                build job: 'devel/dl4j/all-deeplearning4j-android-x86', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-x86"],
                    [$class: 'BooleanParameterDefinition', name:'CBUILD', value: "${CBUILD}"]]
             },
            "Stream 3 android-arm" : {
                build job: 'devel/dl4j/all-deeplearning4j-android-arm', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-arm"],
                    [$class: 'BooleanParameterDefinition', name:'CBUILD', value: "${CBUILD}"]]
            },
            "Stream 4 windows-x86_64" : {
                build job: 'devel/dl4j/all-deeplearning4j-windows-x86_64', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "windows-x86_64"],
                    [$class: 'BooleanParameterDefinition', name:'CBUILD', value: "${CBUILD}"]]
            }
        )
    }
}
