node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    stage ('BuildDockerImages') {
        sh'''
            export JENKINSUID=`id -u`
            export JENKINSGID=`id -g`
            cp -a docker/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80} /tmp/
            echo "RUN groupadd jenkins -g $JENKINSGID && useradd -u $JENKINSUID -g $JENKINSGID -m jenkins" | tee -a /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}/Dockerfile
            cat /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}/Dockerfile
        '''
		parallel (
		    "Stream 0 BuildImageCentos6Cuda80" : {
    			def centos6cuda80 = docker.build ('centos6cuda80','/tmp/centos6cuda80')
		    },
            "Stream 1 BuildImageCentos6Cuda75" : {
                def centos6cuda75 = docker.build ('centos6cuda75','/tmp/centos6cuda75')
            },
            "Stream 2 BuildImageUbuntu14Cuda80" : {
                def ubuntu14cuda80 = docker.build ('ubuntu14cuda80','/tmp/ubuntu14cuda80')
            },
            "Stream 3 BuildImageUbuntu14Cuda75" : {
                def ubuntu14cuda75 = docker.build ('ubuntu14cuda75','/tmp/ubuntu14cuda75')
            },
        )
        sh "rm -f /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}/Dockerfile"
    }

    stage("TestBuiltImages") {
        dockerParams = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
        parallel (
            "Stream 0 Test-centos6cuda80" : {
                docker.image('centos6cuda80').inside(dockerParams) {
                    sh '''
                    java -version
                    mvn -version
                    /opt/sbt/bin/sbt sbt-version
                    source /opt/rh/devtoolset-3/enable
                    cmake --version
                    gcc -v
                    '''
                }
            },
            "Stream 1 Test-centos6cuda75" : {
                docker.image('centos6cuda75').inside(dockerParams) {
                    sh '''
                    java -version
                    mvn -version
                    sbt sbt-version
                    source /opt/rh/devtoolset-3/enable
                    cmake --version
                    gcc -v
                    '''
                }
            },
            "Stream 2 Test-ubuntu14cuda75" : {
                docker.image('ubuntu14cuda75').inside(dockerParams) {
                    sh '''
                    java -version
                    mvn -version
                    sbt sbt-version
                    cmake --version
                    gcc -v
                    '''
                }
            },
            "Stream 3 Test-ubuntu14cuda75" : {
                docker.image('ubuntu14cuda75').inside(dockerParams) {
                    sh '''
                    java -version
                    mvn -version
                    sbt sbt-version
                    cmake --version
                    gcc -v
                    '''
                }
            }
        )
    }
}
