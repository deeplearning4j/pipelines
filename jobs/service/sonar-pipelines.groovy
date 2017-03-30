properties([
        [$class: "ParametersDefinitionProperty", parameterDefinitions:
                [
                        [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "1.1", description: "Pipeline scripts version"],
                        [$class: "StringParameterDefinition", name: "BRANCH", defaultValue: "master", description: "Branch to be checked"],
                        [$class: "ChoiceParameterDefinition", name: "PLATFORM", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Run sonar on architecture"]
                ]
        ]
])

env.SONAR_SERVER = "SonarQubeServer"
// env.SONAR_SCANNER = "SS28"
// env.SONAR_SCANNER = "SS29"
env.SONAR_SCANNER = "SS30"
env.SONAR_MS_SCANNER = "SSMS22"


node(PLATFORM) {

    step([$class: 'WsCleanup'])

    checkout scm

    stage('Check sources with SonarQube') {
      def scannerHome = tool 'SS30';
      long epoch = System.currentTimeMillis()/1000;
      dir("${WORKSPACE}") {
        if (isUnix()) {
          def scannerHome = tool "${SONAR_SCANNER}";
            withSonarQubeEnv("${SONAR_SERVER}") {
              sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=deeplearning4j:pipelines-${BRANCH}\
                  -Dsonar.projectName=${PLATFORM}:pipelines-${BRANCH} -Dsonar.projectVersion=${VERSION}-${epoch} \
                  -Dsonar.sources=."
                // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
            }
        } else {
            def scannerHome = tool "${SONAR_SCANNER}";
            withSonarQubeEnv("${SONAR_SERVER}") {
              bat("${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=deeplearning4j:pipelines-${BRANCH}\
                  -Dsonar.projectName=${PLATFORM}:pipelines-${BRANCH} -Dsonar.projectVersion=${VERSION}-${epoch} \
                  -Dsonar.sources=.")

        }
      }
    }
    // step([$class: 'WsCleanup'])
  }
}
