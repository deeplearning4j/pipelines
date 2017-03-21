env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node("${PLATFORM_NAME}") {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        properties([
                [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
                [$class: "ParametersDefinitionProperty", parameterDefinitions:
                        [
                                [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.8.1-SNAPSHOT", description: "Deeplearning component release version"],
                                [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Build project on architecture"],
                                [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                                [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro081", description: "Default Git branch value"],
                                [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                                [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "sonatype\nnexus\njfrog\nbintray", description: "Profile type"],
                                [$class: "BooleanParameterDefinition", name: "PUSH_LIBND4J_LOCALREPO", defaultValue: false, description: "Select to push libnd4j to local staging repo"]
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
            // def isSnapshot = VERSION.endsWith('SNAPSHOT')

            if (isSnapshot) {
                echo "End of building and publishing of the ${LIBPROJECT}-${VERSION}"
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
}
