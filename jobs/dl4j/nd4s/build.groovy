node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Set docker image and parameters for current platform
    functions.def_docker()

    stage("${ND4S_PROJECT}") {
      load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      // def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${ND4S_PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${ND4S_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${ND4S_PROJECT}")
      }

    }
    
    // step([$class: 'WsCleanup'])

}
