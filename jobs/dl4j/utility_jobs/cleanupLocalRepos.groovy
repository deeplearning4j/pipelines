pipeline {

    agent none

    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
    }

    environment {
        // for support with jobs/dl4j/vars.groovy
        VERSION = "nonempty"
    }

    stages {
        stage('clean-userContent-linux-x86_64') {
            agent { node { label 'linux-x86_64' } }
            steps {
                script {
                    load "jobs/dl4j/vars.groovy"
                }
                sh("rm -rf $JENKINS_DOCKER_M2DIR/repository")
                sh("rm -rf $JENKINS_DOCKER_SBTDIR/cache")
            }
        }

        stage('clean-userContent-linux-ppc64le') {
            agent { node { label 'linux-ppc64le' } }
            steps {
                script {
                    load "jobs/dl4j/vars.groovy"
                }
                sh("rm -rf $JENKINS_DOCKER_M2DIR/repository")
            }
        }
    }

    post {
        always {
            echo "Build of ${currentBuild.fullDisplayName} is ${currentBuild.result}"
        }
    }
}
