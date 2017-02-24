node {

    stage ('Checkout') {
        checkout scm
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    def builders = [:]
    for (image in images) {
        println image
            if ( image.dockerNode ) {
                label = image.dockerNode
                builders[image.name] = {
                    node(label) {
                        stage ("Build " + image.name) {
                            docker.build (image.registry + "/" + image.name,"docker/" + image.name)
                        }
                    }
                }
            }
        }
        println builders
        parallel builders
}


    // def builders = [:]
    // for (i in images) {
    //     def index = i
    //     builders[index] = {
    //         stage ("Build ${index}") {
    //             docker.build ("${dockerRegistry}/${index}","docker/${index}")
    //         }
    //         stage ("Test ${index}") {
    //             docker.image("${dockerRegistry}/${index}").inside(dockerParamsTest) {
    //                 sh '''
    //                 java -version
    //                 mvn -version
    //                 /opt/sbt/bin/sbt sbt-version
    //                 if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
    //                 cmake --version
    //                 gcc -v
    //                 '''
    //             }
    //         }
    //         stage ("Push ${index}") {
    //             if ( PUSH_TO_REGISTRY.toBoolean() ) {
    //                 withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: 'https://${dockerRegistry}']) {
    //                     docker.image("${dockerRegistry}/${index}").push 'latest'
    //                 }
    //             } else {
    //                 echo "Skipping push to registry"
    //             }
    //         }
    //     }
    // }
