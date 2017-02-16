node('sshlocal') {

    step([$class: 'WsCleanup'])

    checkout scm

    stage('Check sources with SonarQube') {
      def scannerHome = tool 'SS28';
      long epoch = System.currentTimeMillis()/1000;
      dir("${WORKSPACE}") {
        withSonarQubeEnv('SonarQubeServer') {
          sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=deeplearning4j:pipelines \
              -Dsonar.projectName=pipelines -Dsonar.projectVersion=${epoch} \
              -Dsonar.sources=."
              // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
        }
      }
    }

    step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"

}
