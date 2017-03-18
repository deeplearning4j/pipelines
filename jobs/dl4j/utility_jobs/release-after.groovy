env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node(PLATFORM_NAME) {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        properties([
                [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
                [$class: "ParametersDefinitionProperty", parameterDefinitions:
                        [
                                [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "", description: "Deeplearning component  version"],
//                                [$class: "StringParameterDefinition", name: "TAG_NAME", defaultValue: "", description: "Deeplearning component desired tag name"],
//                                [$class: "StringParameterDefinition", name: "STAGE_REPO_ID", defaultValue: "", description: "Staging repository Id"],
                                [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\njfrog\nbintray\nsonatype", description: "Profile type"]

                        ]
                ]
        ])

        stage("CLOSE staging repository") {
            if (STAGE_REPO_ID.length() > 0) {
                functions.close_staging_repository("${PROFILE_TYPE}")
            } else {
                echo '[ INFO ] No "Staging repository Id" was provided'
                echo '[ INFO ] Step will be skipped'
            }
        }
    }
}