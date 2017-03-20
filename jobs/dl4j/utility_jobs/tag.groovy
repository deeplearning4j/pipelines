properties([[$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "33"]],
            [$class: "ParametersDefinitionProperty", parameterDefinitions: [
                    [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                    [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Build project on architecture"],
                    [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                    [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"]
            ]]
])

node("master") {
    step([$class: 'WsCleanup'])

    checkout scm

    load "jobs/dl4j/vars.groovy"
    env.CREATE_TAG = env.CREATE_TAG ?: "true"
    functions = load "jobs/dl4j/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    stage("Clone-Progects-Repositories") {
        functions.get_project_code("${LIBPROJECT}")
        functions.get_project_code("${PROJECT}")
        functions.get_project_code("${DATAVEC_PROJECT}")
        functions.get_project_code("${DEEPLEARNING4J_PROJECT}")
        functions.get_project_code("${ARBITER_PROJECT}")
        functions.get_project_code("${ND4S_PROJECT}")
        functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
        functions.get_project_code("${RL4J_PROJECT}")
        functions.get_project_code("${SCALNET_PROJECT}")
    }

    stage("Clone-Progects-Repositories") {
        functions.tag("${LIBPROJECT}")
        functions.tag("${PROJECT}")
        functions.tag("${DATAVEC_PROJECT}")
        functions.tag("${DEEPLEARNING4J_PROJECT}")
        functions.tag("${ARBITER_PROJECT}")
        functions.tag("${ND4S_PROJECT}")
        functions.tag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.tag("${RL4J_PROJECT}")
        functions.tag("${SCALNET_PROJECT}")
    }
}
echo 'MARK: end of tag.groovy'
