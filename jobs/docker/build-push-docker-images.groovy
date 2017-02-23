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

def getPlatformList(list) {
        def tmpList = []
        for (i in list) {
            platform = i.split("-", 2)[1]
            tmpList.add(platform);
        }
        tmpList.unique()
        tmpList.sort()
        return tmpList
}

node {
    def s = "centos6cuda80-linux-x86_64,centos6cuda75-linux-x86_64,ubuntu14-linux-ppc64le,ubuntu16-linux-ppc64le"
    def iList = strToList(s)
    println iList
    def nodeList = getPlatformList(iList)
    println nodeList
    def builders22 = [:]
    for (node in nodeList) {
        for (image in iList){
            if ( image.contains(node)) {
                println "${node} ${image}"
                builders22[image] = {
                    node("${node}") {
                        stage ("Build ${image}") {
                            // docker.build ("${dockerRegistry}/${image}","docker/${image}")
                            println test
                        }
                    }
                }
            }
        }
    }
    parallel builders22
}

// node {
//     stage ('Checkout') {
//         checkout scm
//     }
//
//     stage ("ResolvePlatformDependencies") {
//         echo "Load variables"
//         load "jobs/docker/vars_docker.groovy"
//
//         // def images = ['centos6cuda80', 'centos6cuda75']
//         def builders = [:]
//         if ( DOCKER_IMAGES.size() > 0 ) {
//             images = strToList(DOCKER_IMAGES)
//         }
//         println DOCKER_IMAGES
//     }
//
// }
// node("${DOCKER_NODE}") {
//
//     for (i in images) {
//         def index = i
//         builders[index] = {
//             stage ("Build ${index}") {
//                 docker.build ("${dockerRegistry}/${index}","docker/${index}")
//             }
//             stage ("Test ${index}") {
//                 docker.image("${dockerRegistry}/${index}").inside(dockerParamsTest) {
//                     sh '''
//                     java -version
//                     mvn -version
//                     /opt/sbt/bin/sbt sbt-version
//                     if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
//                     cmake --version
//                     gcc -v
//                     '''
//                 }
//             }
//             stage ("Push ${index}") {
//                 if ( PUSH_TO_REGISTRY.toBoolean() ) {
//                     withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: 'https://${dockerRegistry}']) {
//                         docker.image("${dockerRegistry}/${index}").push 'latest'
//                     }
//                 } else {
//                     echo "Skipping push to registry"
//                 }
//             }
//         }
//     }
//     parallel builders
// }
