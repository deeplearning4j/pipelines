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
        println image.dockerNode + " " + image.name
        println label + " " + xname
        builders[xname] = {
            node(label) {
                stage ("Build " + xname) {
                    unstash 'docker'
                    println label + xname
                    docker.build (xregistry + "/" + xname,"docker/" + xname)
                }
                stage ("Test " + xname) {
                    docker.image(xregistry + "/" + xname,"docker/" + xname).inside(dockerParamsTest) {
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
                stage ("Push " + xname) {
                    if ( PUSH_TO_REGISTRY.toBoolean() ) {
                        withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: "https://" + xregistry]) {
                            docker.image(xregistry/xname).push 'latest'
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
