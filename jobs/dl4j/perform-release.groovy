properties([
        [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
        [$class: "ParametersDefinitionProperty", parameterDefinitions:
                [
                        [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                        // [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86", description: "Build project on architecture"],
                        [$class: "BooleanParameterDefinition", name: "TESTS", defaultValue: false, description: "Select to run tests during mvn execution"],
                        [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                        // [$class: "BooleanParameterDefinition", name: "CREATE_TAG", defaultValue: false, description: "Select to create tag for release in git repository"],
                        // [$class: "StringParameterDefinition", name: "ND4J_VERSION", defaultValue: "", description: "Set preferred nd4j version, leave it empty to use VERSION"],
                        // [$class: "StringParameterDefinition", name: "DL4J_VERSION", defaultValue: "", description: "Set preferred dl4j version, leave it empty to use VERSION"],
                        // [$class: "StringParameterDefinition", name: "DATAVEC_VERSION", defaultValue: "", description: "Set preferred datavec version, leave it empty to use VERSION"],
                        // [$class: "ChoiceParameterDefinition", name: "SCALA_VERSION", choices: "2.10\n2.11", description: "Scala version definition"],
                        // [$class: "ChoiceParameterDefinition", name: "CUDA_VERSION", choices: "7.5\n8.0", description: "Cuda version definition"],
                        [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                        [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                        // [$class: "StringParameterDefinition", name: "PDIR", defaultValue: "jobs/dl4j", description: "Path to groovy scripts"],
                        [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\nlocal-jfrog\njfrog\nbintray\nsonatype", description: "Profile type"]
                ]
        ]
])

env.PDIR = "jobs/dl4j"

node("master") {

  stage("BuildBaseLibs") {
        parallel (
            // "Stream 0 x86_64" : {
            //     build job: 'devel/dl4j/amd64/base-libs', parameters:
            //         [[$class: 'StringParameterValue', name:'GIT_BRANCHNAME', value: GIT_BRANCHNAME],
            //         [$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-x86_64"]]
            // },
            // "Stream 1 ppc64le" : {
            //     build job: 'devel/dl4j/ppc/base-libs', parameters:
            //         [[$class: 'StringParameterValue', name:'GIT_BRANCHNAME', value: GIT_BRANCHNAME],
            //         [$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-ppc64le"]]
            // },
            "Stream 2 android-x86" : {
                build job: 'devel/dl4j/x86android/x86-build-stash', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-x86"]]
            },
            "Stream 3 android-arm" : {
                build job: 'devel/dl4j/arm/ARM-build-stash', parameters:
                    [[$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "android-arm"]]
            }
        )
    }
}

node("linux-x86_64") {

    step([$class: 'WsCleanup'])
    checkout scm

    echo "Load variables"
    load "${PDIR}/vars.groovy"

    echo "load functions"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Create .m2 direcory
    // functions.dirm2()

    // Set docker image and parameters for current platform
    def PLATFORM_NAME = "linux-x86_64"
    functions.def_docker()

    stage("CHECK UNSTASH") {
          unstash 'cpu-blasbuild-arm'
          unstash 'cpu-blasbuild-x86'
          unstash 'cpu-blas-arm'
          unstash 'cpu-blas-x86'

          sh("ls -al")

      }

    }

    echo 'MARK: end of perform-release.groovy'
}
