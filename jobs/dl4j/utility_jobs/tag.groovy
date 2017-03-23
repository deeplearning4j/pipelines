properties([[$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "33"]],
            [$class: "ParametersDefinitionProperty", parameterDefinitions: [
                    [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.8.1-SNAPSHOT", description: "Deeplearning component release version"],
                    [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Build project on architecture"],
                    [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "master", description: "Default Git branch value"],
                    [$class: "BooleanParameterDefinition", name: "TAG", defaultValue: false, description: "Select to push tags to GitHub"],
                    [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"]
            ]]
])

node(PLATFORM_NAME) {
    step([$class: 'WsCleanup'])

    checkout scm

    load "jobs/dl4j/vars.groovy"
    env.CREATE_TAG = env.CREATE_TAG ?: "true"
    functions = load "jobs/dl4j/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    stage("Clone-Progects-Repositories") {
        functions.express("Downloading ${LIBPROJECT} repository")
        functions.get_project_code("${LIBPROJECT}")

        functions.express("Downloading ${PROJECT} repository")
        functions.get_project_code("${PROJECT}")

        functions.express("Downloading ${DATAVEC_PROJECT} repository")
        functions.get_project_code("${DATAVEC_PROJECT}")

        functions.express("Downloading ${DEEPLEARNING4J_PROJECT} repository")
        functions.get_project_code("${DEEPLEARNING4J_PROJECT}")

        functions.express("Downloading ${ARBITER_PROJECT} repository")
        functions.get_project_code("${ARBITER_PROJECT}")

        functions.express("Downloading ${ND4S_PROJECT} repository")
        functions.get_project_code("${ND4S_PROJECT}")

        functions.express("Downloading ${GYM_JAVA_CLIENT_PROJECT} repository")
        functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")

        functions.express("Downloading ${RL4J_PROJECT} repository")
        functions.get_project_code("${RL4J_PROJECT}")

        // enable scalnet after 0.8.0 releasße
        // functions.express("Downloading ${SCALNET_PROJECT} repository")
        // functions.get_project_code("${SCALNET_PROJECT}")

    }

    stage("Tag-Repositories") {
        functions.express("Adding tag  ${LIBPROJECT}-${VERSION} to repository")
        functions.tag("${LIBPROJECT}")

        functions.express("Adding tag  ${PROJECT}-${VERSION} to repository")
        functions.tag("${PROJECT}")

        functions.express("Adding tag  ${DATAVEC_PROJECT}-${VERSION} to repository")
        functions.tag("${DATAVEC_PROJECT}")

        functions.express("Adding tag  ${DEEPLEARNING4J_PROJECT}-${VERSION} to repository")
        functions.tag("${DEEPLEARNING4J_PROJECT}")

        functions.express("Adding tag  ${ARBITER_PROJECT}-${VERSION} to repository")
        functions.tag("${ARBITER_PROJECT}")

        functions.express("Adding tag  ${ND4S_PROJECT}-${VERSION} to repository")
        functions.tag("${ND4S_PROJECT}")

        functions.express("Adding tag  ${GYM_JAVA_CLIENT_PROJECT}-${VERSION} to repository")
        functions.tag("${GYM_JAVA_CLIENT_PROJECT}")

        functions.express("Adding tag  ${RL4J_PROJECT}-${VERSION} to repository")
        functions.tag("${RL4J_PROJECT}")

        // functions.express("Adding tag  ${SCALNET_PROJECT}-${VERSION} to repository")
        // functions.tag("${SCALNET_PROJECT}")
    }
}
ansiColor('xterm') {
    echo "\033[42m MARK: end of tag.groovy \033[0m"
    // echo 'MARK: end of tag.groovy'
}
