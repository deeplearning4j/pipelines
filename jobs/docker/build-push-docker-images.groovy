node {
    stage ('Checkout') {
        checkout scm
        stash includes: 'docker/', name: 'docker'
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    def builders = [:]

    for (image in images) {
        def dockerImage
        def label = image.dockerNode
        def xname = image.name
        def xregistry = image.registry

        echo "${image.dockerNode} ${image.name}"
        echo "${label} ${xname}"

        builders[xname] = {

            node(label) {

                stage ("Build ${xname}") {
                    unstash 'docker'
                    dockerImage = docker.build("skymindops/pipelines:${xname}","docker/${xname}")
                }

                stage ("Test ${xname}") {
                    dockerImage.inside {
                        sh '''
                        java -version
                        mvn -version
                        #/opt/sbt/bin/sbt sbt-version
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable; fi
                        cmake --version
                        gcc -v
                        '''
                    }
                }

                stage ("Push ${xname}") {
                    if ( PUSH_TO_REGISTRY.toBoolean() ) {
                        docker.withRegistry("https://${xregistry}", 'dockerRegistry') {
                            dockerImage.push()
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
