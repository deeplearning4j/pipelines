node("${DOCKER_NODE}") {

    echo "Cleanup WS"
    step([$class: 'WsCleanup'])

    // dockerParams = "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${WORKSPACE}/.m2:/home/jenkins/.m2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

    checkout scm

    load "${PDIR}/vars.groovy"

    functions = load "${PDIR}/functions.groovy"

    functions.rm()

    sh ("mkdir ${WORKSPACE}/.m2 || true")

    stage("${GYM_JAVA_CLIENT_PROJECT}") {
      load "${PDIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {
      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(isSnapshot) {
        echo "End of building and publishing of the ${GYM_JAVA_CLIENT_PROJECT}-${RELEASE_VERSION}"
      }
      else {
        // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${GYM_JAVA_CLIENT_PROJECT}-${RELEASE_VERSION} ?"
        }

        functions.release("${GYM_JAVA_CLIENT_PROJECT}")
      }

    }

    step([$class: 'WsCleanup'])

}
