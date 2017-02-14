node("${DOCKER_NODE}") {

    println "Cleanup WS"
    step([$class: 'WsCleanup'])

    // dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

    checkout scm
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Remove .git folder from workspace
    sh("rm -rf ${WORKSPACE}/.git")
    sh("rm -rf ${WORKSPACE}/docs")
    sh("rm -rf ${WORKSPACE}/imgs")
    sh("rm -rf ${WORKSPACE}/ansible")
    sh("rm -f ${WORKSPACE}/.gitignore")
    sh("rm -f ${WORKSPACE}/README.md")
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    load "${PDIR}/vars.groovy"

    functions = load "${PDIR}/functions.groovy"

    sh ("mkdir ${WORKSPACE}/.m2 || true")

    stage("${DATAVEC_PROJECT}") {
      load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(!isSnapshot) {
      // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${DATAVEC_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${DATAVEC_PROJECT}")
      }
      else {
        println "End of building and publishing of the ${DATAVEC_PROJECT}-${RELEASE_VERSION}"
      }

    }
    step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"

}
