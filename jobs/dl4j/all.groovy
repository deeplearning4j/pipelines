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

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // LIBPROJECT building is circumvented in nd4j-docker.groovy
    // in the resolveDependencies stage
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    /*
    stage("${LIBPROJECT}") {
        load "${AMD64DIR}/${LIBPROJECT}/${LIBPROJECT}-docker.groovy"
    }
    */

    stage("${PROJECT}") {
        load "${AMD64DIR}/${PROJECT}/${PROJECT}-docker.groovy"
    }

    stage("${DATAVEC_PROJECT}") {
      load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}-docker.groovy"
    }

    stage("${DEEPLEARNING4J_PROJECT}") {
        load "${AMD64DIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}-docker.groovy"
    }

    stage ("${ARBITER_PROJECT}") {
      load "${AMD64DIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}-docker.groovy"
    }

    stage("${ND4S_PROJECT}") {
        load "${AMD64DIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    }

    stage("${GYM_JAVA_CLIENT_PROJECT}") {
      load "${AMD64DIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}-docker.groovy"
    }

    stage("${RL4J_PROJECT}") {
      load "${AMD64DIR}/${RL4J_PROJECT}/${RL4J_PROJECT}-docker.groovy"
    }

    // depends on nd4j and deeplearning4j-core
    stage("${SCALNET_PROJECT}") {
    	load "${AMD64DIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}-docker.groovy"
    }


    stage('RELEASE') {

      def isSnapshot = RELEASE_VERSION.endsWith('SNAPSHOT')

      if(!isSnapshot) {
      // timeout(time:1, unit:'HOURS') {
        timeout(20) {
            input message:"Approve release of version ${RELEASE_VERSION} ?"
        }

        // functions.release("${LIBPROJECT}")
        functions.release("${PROJECT}")
        functions.release("${DATAVEC_PROJECT}")
        functions.release("${DEEPLEARNING4J_PROJECT}")
        functions.release("${ARBITER_PROJECT}")
        functions.release("${ND4S_PROJECT}")
        functions.release("${GYM_JAVA_CLIENT_PROJECT}")
        functions.release("${RL4J_PROJECT}")
        functions.release("${SCALNET_PROJECT}")
      }
      else {
        println "End of building and publishing of the ${RELEASE_VERSION}"
      }

    }

    println "Cleanup WS"
    sh "rm -rf $HOME/.sonar"
    step([$class: 'WsCleanup'])

    echo 'MARK: end of allcc.groovy'
}
