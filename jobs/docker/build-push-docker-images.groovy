node {

    stage ('Checkout') {
        checkout scm
        stash includes: 'docker/', name: 'docker'
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    def builders = [:]

    for (image in images) {
        println image
        def label = image.dockerNode
        def xname = image.name
        def xregistry = image.registry
        def parent = image.parentImage
        echo "${image.dockerNode} ${image.name}"
        echo "${label} ${xname}"
        builders[xname] = {
            node(label) {
                stage ("Build ${xname}") {
                    unstash 'docker'
                    docker.image(parent).pull()
                    docker.build ("${xregistry}/${xname}","docker/${xname}")
                }
                stage ("Test ${xname}") {
                    docker.image("${xregistry}/${xname}").inside {
                        sh '''
                        java -version
                        mvn -version
                        #/opt/sbt/bin/sbt sbt-version
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                        cmake --version
                        gcc -v
                        '''
                    }
                }
                stage ("Push ${xname}") {
                    if ( PUSH_TO_REGISTRY.toBoolean() ) {
                      withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: "https://${xregistry}"]) {
                        docker.withRegistry("https://${xregistry}", "https://${xregistry}").image("${xregistry}/${xname}").push 'latest'
                      }
                    } else {
                        echo "Skipping push to registry"
                    }
                }
            }
        }
    }
    parallel builders
}
