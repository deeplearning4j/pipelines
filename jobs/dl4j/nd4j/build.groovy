node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Create .m2 direcory
    functions.dirm2()

    // Set docker image and parameters for current platform
    functions.def_docker()

    stage("${PROJECT}") {
      load "${PDIR}/${PROJECT}/${PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      // def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${PROJECT}")
      }

    }

    // step([$class: 'WsCleanup'])

}
