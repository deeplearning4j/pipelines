node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    sh ("mkdir ${WORKSPACE}/.m2 || true")

    stage("${PROJECT}") {
      load "${PDIR}/${PROJECT}/${PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

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

    step([$class: 'WsCleanup'])

}
