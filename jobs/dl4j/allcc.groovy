node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    // Remove .git folder from workspace
    sh("rm -rf ${WORKSPACE}/.git")
    sh("rm -f ${WORKSPACE}/.gitignore")
    sh("rm -rf ${WORKSPACE}/docs")
    sh("rm -rf ${WORKSPACE}/imgs")
    sh("rm -rf ${WORKSPACE}/ansible")
    sh("rm -f ${WORKSPACE}/README.md")

    sh("env")
    sh("ls -al")

    sh ("mkdir ${WORKSPACE}/.m2 || true")

    echo "Load variables"
    load "${PDIR}/vars.groovy"

    echo "load functions"
    functions = load "${PDIR}/functions.groovy"

    stage("${LIBPROJECT}") {
        load "${PDIR}/${LIBPROJECT}/${LIBPROJECT}-docker.groovy"
    }

    stage("${PROJECT}") {
        load "${PDIR}/${PROJECT}/${PROJECT}-cc-docker.groovy"
    }

    stage("${DATAVEC_PROJECT}") {
      load "${PDIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}-docker.groovy"
    }

    stage("${DEEPLEARNING4J_PROJECT}") {
        load "${PDIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}-docker.groovy"
    }

    stage ("${ARBITER_PROJECT}") {
      load "${PDIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}-docker.groovy"
    }

    stage("${ND4S_PROJECT}") {
        load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    }

    stage("${GYM_JAVA_CLIENT_PROJECT}") {
      load "${PDIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}-docker.groovy"
    }

    stage("${RL4J_PROJECT}") {
      load "${PDIR}/${RL4J_PROJECT}/${RL4J_PROJECT}-docker.groovy"
    }

    // depends on nd4j and deeplearning4j-core
    stage("${SCALNET_PROJECT}") {
    	load "${PDIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}-docker.groovy"
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

    sh "rm -rf $HOME/.sonar"
    step([$class: 'WsCleanup'])

    echo 'MARK: end of allcc.groovy'
}
