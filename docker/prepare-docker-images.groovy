node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    stage ('BuildDockerImages') {
        sh '''
        docker pull intropro/nvidia-docker:7.5-builder-ubuntu14.04
        docker pull intropro/nvidia-docker:8.0-builder-ubuntu14.04
        docker pull intropro/nvidia-docker:7.5-builder-centos6
        docker pull intropro/nvidia-docker:8.0-builder-centos6
        export JENKINSUID=`id -u`
        export JENKINSGID=`id -g`
        mkdir -p /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}
        echo "FROM intropro/nvidia-docker:7.5-builder-ubuntu14.04" > /tmp/ubuntu14cuda75/Dockerfile
        echo "FROM intropro/nvidia-docker:8.0-builder-ubuntu14.04" > /tmp/ubuntu14cuda80/Dockerfile
        echo "FROM intropro/nvidia-docker:7.5-builder-centos6" > /tmp/centos6cuda75/Dockerfile
        echo "FROM intropro/nvidia-docker:8.0-builder-centos6" > /tmp/centos6cuda80/Dockerfile
        echo "RUN groupadd jenkins -g $JENKINSGID && useradd -u $JENKINSUID -g $JENKINSGID -m jenkins" | tee -a /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}/Dockerfile
        cat /tmp/{ubuntu14cuda75,ubuntu14cuda80,centos6cuda75,centos6cuda80}/Dockerfile'''
		parallel (
		    "Stream 0 PrepareImageCentos6Cuda80" : {
    			docker.build ('centos6cuda80','/tmp/centos6cuda80')
		    },
            "Stream 1 PrepareImageCentos6Cuda75" : {
                docker.build ('centos6cuda75','/tmp/centos6cuda75')
            },
            "Stream 2 PrepareImageUbuntu14Cuda80" : {
                docker.build ('ubuntu14cuda80','/tmp/ubuntu14cuda80')
            },
            "Stream 3 PrepareImageUbuntu14Cuda75" : {
                docker.build ('ubuntu14cuda75','/tmp/ubuntu14cuda75')
            }
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
