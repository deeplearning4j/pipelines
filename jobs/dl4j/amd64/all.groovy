timestamps {
    node('amd64&&g2&&ubuntu16') {
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Commented WsCleanup Step to minimize time for build
        // step([$class: 'WsCleanup'])

        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Discard old builds by keeping log of 5 last
//        properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        checkout scm
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Remove .git folder from workspace
        sh("rm -rf ${WORKSPACE}/.git")

        // Some debugging

        sh("pwd")
        sh("ls -al")


        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

        stage("${LIBPROJECT}") {
          load "${AMD64DIR}/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }

        stage("${PROJECT}") {
          load "${AMD64DIR}/${PROJECT}/${PROJECT}.groovy"
        }


        def builds = [:]

            builds["${DATAVEC_PROJECT}"] = {
              load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
            }

            builds["${DEEPLEARNING4J_PROJECT}"] = {
              load "${AMD64DIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
            }

            builds["${ARBITER_PROJECT}"] = {
              load "${AMD64DIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
            }

            builds["${ND4S_PROJECT}"] = {
              load "${AMD64DIR}/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
            }

            builds["${GYM_JAVA_CLIENT_PROJECT}"] = {
              load "${AMD64DIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
            }

            builds["${RL4J_PROJECT}"] = {
              load "${AMD64DIR}/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
            }

        parallel builds

/*
        stage("${DATAVEC_PROJECT}") {
          load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
        }

        stage("${DEEPLEARNING4J_PROJECT}") {
          load "${AMD64DIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
        }

        stage ("${ARBITER_PROJECT}") {
          load "${AMD64DIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
        }

        stage("${ND4S_PROJECT}") {
          load "${AMD64DIR}/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
        }

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
          load "${AMD64DIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
        }

        stage("${RL4J_PROJECT}") {
          load "${AMD64DIR}/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
        }
*/
        // depends on nd4j and deeplearning4j-core
        stage("${SCALNET_PROJECT}") {
        	load "${AMD64DIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
        }

/*

    stage('RELEASE') {
      // timeout(time:1, unit:'HOURS') {
      timeout(10) {
          input message:"Approve release of version ${RELEASE_VERSION} ?"
      }

      functions.release("${LIBPROJECT}")
      functions.release("${PROJECT}")
      functions.release("${DATAVEC_PROJECT}")
      functions.release("${DEEPLEARNING4J_PROJECT}")
      functions.release("${ARBITER_PROJECT}")
      functions.release("${ND4S_PROJECT}")
      functions.release("${GYM_JAVA_CLIENT_PROJECT}")
      functions.release("${RL4J_PROJECT}")
      functions.release("${SCALNET_PROJECT}")
    }

    // step([$class: 'WsCleanup'])
    sh "rm -rf $HOME/.sonar"*//*
*/

        // Messages for debugging
        echo 'MARK: end of all.groovy'
    }
}
