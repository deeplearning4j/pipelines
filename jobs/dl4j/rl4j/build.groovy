node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    // dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

    checkout scm
    // Remove .git folder from workspace
    sh("rm -rf ${WORKSPACE}/.git")
    sh("rm -f ${WORKSPACE}/.gitignore")
    sh("rm -rf ${WORKSPACE}/docs")
    sh("rm -rf ${WORKSPACE}/imgs")
    sh("rm -rf ${WORKSPACE}/ansible")
    sh("rm -f ${WORKSPACE}/README.md")

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    sh ("mkdir ${WORKSPACE}/.m2 || true")


    stage("${RL4J_PROJECT}") {
      load "${PDIR}/${RL4J_PROJECT}/${RL4J_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(!isSnapshot) {
      // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${RL4J_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${RL4J_PROJECT}")
      }
      else {
        println "End of building and publishing of the ${RL4J_PROJECT}-${RELEASE_VERSION}"
      }

    }

    step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"

}
