timestamps {
  node ('master') {
    step([$class: 'WsCleanup'])

    checkout scm

    sh 'env > env.txt'
    readFile('env.txt').split("\r?\n").each {
        println it
    }

    stage('ND4J') {
      load 'jobs/build-01-nd4j.groovy'
    }

    stage('DATAVEC') {
      load 'jobs/build-02-datavec.groovy'
    }

    stage('DEEPLEARNING4J') {
    	load  'jobs/build-03-deeplearning4j.groovy'
    }

    stage('ARBITER') {
    	load 'jobs/build-04-arbiter.groovy'
    }

    stage('ND4S') {
    	load 'jobs/build-05-nd4s.groovy'
    }

    stage('GYM-JAVA-CLIENT') {
    	load 'jobs/build-06-gym-java-client.groovy'
    }

    stage('RL4J') {
    	load 'jobs/build-07-rl4j.groovy'
    }

    stage('SCALNET') {
    	load 'jobs/build-08-scalnet.groovy'
    }

    stage('RELEASE') {
      // timeout(time:1, unit:'HOURS') {
      timeout(10) {
          input message:"Approve release of version ${RELEASE_VERSION} ?"
      }
      echo "Adding tag ${PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${PROJECT}"
      dir("${PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${LIBPROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${LIBPROJECT}"
      dir("${LIBPROJECT}") {
        sh "ls -la ${pwd()}"
        // Define what to do with linbnd4j build
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${LIBPROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${DATAVEC_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${DATAVEC_PROJECT}"
      dir("${DATAVEC_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${DATAVEC_PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${DEEPLEARNING4J_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${DEEPLEARNING4J_PROJECT}"
      dir("${DEEPLEARNING4J_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${DEEPLEARNING4J_PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${ARBITER_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${ARBITER_PROJECT}"
      dir("${ARBITER_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${ARBITER_PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${ND4S_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${ND4S_PROJECT}"
      dir("${ND4S_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${ND4S_PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${GYM_JAVA_CLIENT_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${GYM_JAVA_CLIENT_PROJECT}"
      dir("${GYM_JAVA_CLIENT_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${GYM_JAVA_CLIENT_PROJECT}-$RELEASE_VERSION""
      }

      echo "Adding tag ${RL4J_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${RL4J_PROJECT}"
      dir("${RL4J_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        sh ("echo aaa-${RL4J_PROJECT} ${RELEASE_VERSION}")
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${RL4J_PROJECT}-${RELEASE_VERSION}""
      }

      echo "Adding tag ${SCALNET_PROJECT}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${SCALNET_PROJECT}"
      dir("${SCALNET_PROJECT}") {
        sh "ls -la ${pwd()}"
        // TODO: send command to bintray to mirror release to Maven Central
        sh ("echo ${SCALNET_PROJECT} ${RELEASE_VERSION}")
        sh "git status"
        // DO NOT ENABLE TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        // sh "git tag -a -m "${RL4J_PROJECT}-${RELEASE_VERSION}""
      }
    }
  }

  step([$class: 'WsCleanup'])

  // Messages for debugging
  echo 'MARK: end of pipeline.groovy'

}
