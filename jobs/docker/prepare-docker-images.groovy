node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    stage ('BuildDockerImages') {
        sh '''
        docker pull $dockerRegistry/centos6cuda80
        docker pull $dockerRegistry/ubuntu14cuda80
        export JENKINSUID=`id -u`
        export JENKINSGID=`id -g`
        mkdir -p /tmp/{ubuntu14cuda80,centos6cuda80}
        echo "FROM ${dockerRegistry}/centos6cuda80" > /tmp/centos6cuda80/Dockerfile
        echo "FROM ${dockerRegistry}/ubuntu14cuda80" > /tmp/ubuntu14cuda80/Dockerfile
        echo "RUN groupadd jenkins -g $JENKINSGID && useradd -u $JENKINSUID -g $JENKINSGID -m jenkins" | tee -a /tmp/{ubuntu14cuda80,centos6cuda80}/Dockerfile
        cat /tmp/{ubuntu14cuda80,centos6cuda80}/Dockerfile'''
		parallel (
		    "Stream 0 PrepareImageCentos6Cuda80" : {
    			docker.build ('centos6cuda80','/tmp/centos6cuda80')
		    },
            "Stream 1 PrepareImageUbuntu14Cuda80" : {
                docker.build ('ubuntu14cuda80','/tmp/ubuntu14cuda80')
            }
        )
        sh "rm -f /tmp/{ubuntu14cuda80,centos6cuda80}/Dockerfile"
    }

    stage("TestBuiltImages") {
        parallel (
            "Stream 0 Test-centos6cuda80" : {
                docker.image('centos6cuda80').inside(dockerParamsTest) {
                    sh '''
                    whoami
                    '''
                }
            },
            "Stream 1 Test-ubuntu14cuda80" : {
                docker.image('ubuntu14cuda80').inside(dockerParamsTest) {
                    sh '''
                    whoami
                    '''
                }
            }
        )
    }
}
