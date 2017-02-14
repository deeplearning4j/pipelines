node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Remove .git folder from workspace
    sh("rm -rf ${WORKSPACE}/.git")
    sh("rm -f ${WORKSPACE}/.gitignore")
    sh("rm -rf ${WORKSPACE}/docs")
    sh("rm -rf ${WORKSPACE}/imgs")
    sh("rm -rf ${WORKSPACE}/ansible")
    sh("rm -f ${WORKSPACE}/README.md")
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    sh ("mkdir ${WORKSPACE}/.m2 || true")

    stage("${PROJECT}") {
      load "${PDIR}/${PROJECT}/${PROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(!isSnapshot) {
      // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${PROJECT}")
      }
      else {
        println "End of building and publishing of the ${PROJECT}-${RELEASE_VERSION}"
      }

    }

    step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"

}
