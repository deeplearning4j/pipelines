node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // sh ("mkdir ${WORKSPACE}/.m2 || true")

    stage("${ND4S_PROJECT}") {
      load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(!isSnapshot) {
      // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${ND4S_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${ND4S_PROJECT}")
      }
      else {
        println "End of building and publishing of the ${ND4S_PROJECT}-${RELEASE_VERSION}"
      }

    }

    sh "rm -rf $HOME/.sonar"
    step([$class: 'WsCleanup'])

}
