node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    // dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

    checkout scm

    load "${PDIR}/vars.groovy"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Create .m2 direcory
    functions.dirm2()

    stage("${SCALNET_PROJECT}") {
      load "${PDIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${SCALNET_PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${SCALNET_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${SCALNET_PROJECT}")
      }

    }

    step([$class: 'WsCleanup'])

}
