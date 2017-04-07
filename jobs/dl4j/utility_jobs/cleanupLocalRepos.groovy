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
                sh("du -sh $JENKINS_DOCKER_M2DIR $JENKINS_DOCKER_SBTDIR")
                sh("rm -rf $JENKINS_DOCKER_M2DIR/repository")
                sh("rm -rf $JENKINS_DOCKER_SBTDIR/cache")
                sh("du -sh $JENKINS_DOCKER_M2DIR $JENKINS_DOCKER_SBTDIR")
            }
        }
        stage('clean-userContent-linux-ppc64le') {
            agent { node { label 'linux-ppc64le' } }
            steps {
                script {
                    load "jobs/dl4j/vars.groovy"
                }
                sh("du -sh $JENKINS_DOCKER_M2DIR")
                sh("rm -rf $JENKINS_DOCKER_M2DIR/repository")
                sh("du -sh $JENKINS_DOCKER_M2DIR")
            }
        }
        stage('clean-userContent-macosx-x86_64') {
            agent { node { label 'macosx-x86_64' } }
            steps {
                sh("du -sh $HOME/.m2")
                sh("rm -rf $HOME/.m2/repository")
                sh("du -sh $HOME/.m2")
            }
        }
        stage('clean-userContent-windows-x86_64') {
            agent { node { label 'windows-x86_64' } }
            steps {
                bat'''
                dir C:\\Users\\jenkins\\.m2
                rd /s /q C:\\Users\\jenkins\\.m2\\repository
                dir C:\\Users\\jenkins\\.m2
                '''
            }
        }
    }

    post {
        always {
            echo "Build of ${currentBuild.fullDisplayName} is ${currentBuild.result}"
        }
    }
}
