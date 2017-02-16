node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    def images = ['centos6cuda80', 'centos6cuda75']
    def builders = [:]
    for (i in images) {
        def index = i
        builders[index] = {
            stage ("Build ${index}") {
                docker.build ("${dockerRegistry}/${index}","docker/${index}")
            }
            stage ("Test ${index}") {
                docker.image("${dockerRegistry}/${index}").inside(dockerParamsTest) {
                    sh '''
                    java -version
                    mvn -version
                    /opt/sbt/bin/sbt sbt-version
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                    cmake --version
                    gcc -v
                    '''
                }
            }
            stage ('Push ${index}') {
                withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: 'https://${dockerRegistry}']) {
                    docker.image("${dockerRegistry}/${index}").push 'latest'
                }
            }
        }
    }
    parallel builders
}
