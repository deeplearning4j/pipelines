timestamps {
    node('local-slave') {

        step([$class: 'WsCleanup'])

        checkout scm

        load 'jobs/dl4j/vars.groovy'
        functions = load 'jobs/dl4j/functions.groovy'
        /*
        env.ND4J_VERSION="${RELEASE_VERSION}"
        env.DATAVEC_VERSION="${RELEASE_VERSION}"
        env.DL4J_VERSION="${RELEASE_VERSION}"
        */
        // stage("${LIBPROJECT}") {
        //   // load 'jobs/dl4j/libnd4j/libnd4j.groovy'
        //   load "jobs/dl4j/${LIBPROJECT}/${LIBPROJECT}.groovy"
        // }

        stage("${PROJECT}") {
          // load 'jobs/dl4j/nd4j/nd4j.groovy'
          load "jobs/dl4j/${PROJECT}/build.groovy"
        }

/*
        def builds = [:]

            builds["${DATAVEC_PROJECT}"] = {
              // load 'jobs/dl4j/datavec/datavec.groovy'
              load "jobs/dl4j/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
            }

            builds["${DEEPLEARNING4J_PROJECT}"] = {
              // load  'jobs/dl4j/deeplearning4j/deeplearning4j.groovy'
              load "jobs/dl4j/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
            }

            builds["${ARBITER_PROJECT}"] = {
              // load 'jobs/dl4j/arbiter/arbiter.groovy'
              load "jobs/dl4j/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
            }

            builds["${ND4S_PROJECT}"] = {
              // load 'jobs/dl4j/nd4s/nd4s.groovy'
              load "jobs/dl4j/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
            }

            builds["${GYM_JAVA_CLIENT_PROJECT}"] = {
              // load 'jobs/dl4j/gym-java-client.groovy'
              load "jobs/dl4j/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
            }

            builds["${RL4J_PROJECT}"] = {
              // load 'jobs/dl4j/rl4j.groovy'
              load "jobs/dl4j/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
            }

        parallel builds
*/

    stage("${DATAVEC_PROJECT}") {
      // load 'jobs/dl4j/datavec/datavec.groovy'
      load "jobs/dl4j/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
    }

    stage("${DEEPLEARNING4J_PROJECT}") {
      // load 'jobs/dl4j/deeplearning4j/deeplearning4j.groovy'
      load "jobs/dl4j/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
    }

    stage ("${ARBITER_PROJECT}") {
      // load 'jobs/dl4j/arbiter/arbiter.groovy'
      load "jobs/dl4j/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
    }

    stage("${ND4S_PROJECT}") {
      // load 'jobs/dl4j/nd4s/nd4s.groovy'
      load "jobs/dl4j/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
    }

    stage("${GYM_JAVA_CLIENT_PROJECT}") {
      // load 'jobs/dl4j/gym-java-client.groovy'
      load "jobs/dl4j/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
    }

    stage("${RL4J_PROJECT}") {
      // load 'jobs/dl4j/rl4j.groovy'
      load "jobs/dl4j/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
    }

    // depends on nd4j and deeplearning4j-core
    stage("${SCALNET_PROJECT}") {
    	// load 'jobs/dl4j/scalnet.groovy'
    	load "jobs/dl4j/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
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
