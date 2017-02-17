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

    stage("${LIBPROJECT}") {
        load "${PDIR}/${LIBPROJECT}/${LIBPROJECT}-docker.groovy"
    }

    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${LIBPROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${LIBPROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${LIBPROJECT}")
      }

    }

    step([$class: 'WsCleanup'])

}
