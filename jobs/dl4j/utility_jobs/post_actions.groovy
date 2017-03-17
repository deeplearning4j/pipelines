env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node(PLATFORM_NAME) {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        properties([
                [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
                [$class: "ParametersDefinitionProperty", parameterDefinitions:
                        [
                                [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "", description: "Deeplearning component release version"],
                                [$class: "StringParameterDefinition", name: "TAG_NAME", defaultValue: "", description: "Deeplearning component release version"],
                                [$class: "StringParameterDefinition", name: "STAGE_REPO_ID", defaultValue: "", description: "Staging repository Id"]
                        ]
                ]
        ])
    }
}