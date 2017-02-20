node("${DOCKER_NODE}") {

    echo "Cleanup WS"
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

    stage("${ARBITER_PROJECT}") {
      load "${PDIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      // def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${ARBITER_PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${ARBITER_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${ARBITER_PROJECT}")
      }

    }

    // step([$class: 'WsCleanup'])

}
