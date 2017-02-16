node('sshslave') {

    step([$class: 'WsCleanup'])

    checkout scm

    stage('Check sources with SonarQube') {
      def scannerHome = tool 'SS28';
      dir("${proj}") {
        withSonarQubeEnv('SonarQubeServer') {
          sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${ACCOUNT}:${proj} \
              -Dsonar.projectName=${proj} -Dsonar.projectVersion=${RELEASE_VERSION} \
              -Dsonar.sources=."
              // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
        }
      }
    }

    step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"

}
