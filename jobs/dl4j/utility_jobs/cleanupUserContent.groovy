pipeline {

    agent { node { label 'master' } }

    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }

    stages {
        stage('clean-userContent') {
            steps {
                sh("rm -rf $JENKINS_HOME/userContent/SNAPSHOT-*")
                sh("rm -rf $JENKINS_HOME/userContent/RELEASE-*")
            }
        }
    }

    post {
        always {
            echo "Build of ${currentBuild.fullDisplayName} is ${currentBuild.result}"
        }
    }
}
