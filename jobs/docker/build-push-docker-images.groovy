def strToList(str) {
    if (str.getClass() == String && str.length()>0) {
        tmpList = []
        for ( i in str.split(",")) {
            def item = i
            tmpList.add(item);
        }
        } else {
            error "strToList(): Input arg isn't string or empty, class: ${str.getClass()}, size: ${str.length()}"
        }
        return tmpList
    }

node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"


    def images = ['centos6cuda80', 'centos6cuda75']
    def builders = [:]
    if ( DOCKER_IMAGES.size() > 0 ) {
        images = strToList(DOCKER_IMAGES)
    }
    echo images
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
            stage ("Push ${index}") {
                echo PUSH_TO_REGISTRY
                if ( PUSH_TO_REGISTRY == "true" ) {
                    withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: 'https://${dockerRegistry}']) {
                        docker.image("${dockerRegistry}/${index}").push 'latest'
                    }
                } else {
                    echo "Skipping push to registry"
                }
            }
        }
    }
    parallel builders
}
