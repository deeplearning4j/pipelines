timestamps {
  node ('master') {
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Commented WsCleanup Step to minimize time for build
    //step([$class: 'WsCleanup'])

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Discard old builds by keeping log of 5 last
    properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // legacy part will take a look to it later
    // def GITCREDID = 'github-private-deeplearning4j-id-1'

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Not sure if this step is really required
    checkout scm
/*
    // sh ("env")

    echo "Load Functions"
    functions = load 'jobs/functions.groovy'

    stage('ND4J') {
      load 'jobs/build-01-nd4j.groovy'
    }

    stage('Build Components') {
      def builds = [:]
        builds['DATAVEC'] = {
          load 'jobs/build-02-datavec.groovy'
        }

        builds['DEEPLEARNING4J'] = {
      	   load  'jobs/build-03-deeplearning4j.groovy'
        }

        builds['ARBITER'] = {
        	load 'jobs/build-04-arbiter.groovy'
        }

        builds['ND4S'] = {
        	load 'jobs/build-05-nd4s.groovy'
        }

        builds['GYM-JAVA-CLIENT'] = {
        	load 'jobs/build-06-gym-java-client.groovy'
        }

        builds['RL4J'] = {
        	load 'jobs/build-07-rl4j.groovy'
        }

      parallel builds

    }

    // stage('DATAVEC') {
    //   load 'jobs/build-02-datavec.groovy'
    // }
    //
    // stage('DEEPLEARNING4J') {
    // 	load  'jobs/build-03-deeplearning4j.groovy'
    // }
    //
    // stage('ARBITER') {
    // 	load 'jobs/build-04-arbiter.groovy'
    // }
    //
    // stage('ND4S') {
    // 	load 'jobs/build-05-nd4s.groovy'
    // }
    //
    // stage('GYM-JAVA-CLIENT') {
    // 	load 'jobs/build-06-gym-java-client.groovy'
    // }
    //
    // stage('RL4J') {
    // 	load 'jobs/build-07-rl4j.groovy'
    // }

    // depends on nd4j and deeplearning4j-core
    stage('SCALNET') {
    	load 'jobs/build-08-scalnet.groovy'
    }

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
    sh "rm -rf $HOME/.sonar"*/
  }

  // Messages for debugging
  echo 'MARK: end of pipeline.groovy'
}


// def release(PROJ) {
//   echo "Adding tag ${PROJ}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${PROJ}"
//   dir("${PROJ}") {
//     sshagent(credentials: ["${GITCREDID}"]) {
//       sh "ls -al"
//       echo "hello from ${PROJ}"
//       sh 'git config user.email "jenkins@skymind.io"'
//       sh 'git config user.name "Jenkins"'
//       sh 'git status'
//       // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
//       // sh 'git commit -a -m "Update to version ${RELEASE_VERSION}"'
//       // sh 'git tag -a ${SCALNET_PROJECT}-${RELEASE_VERSION} -m ${SCALNET_PROJECT}-${RELEASE_VERSION}'
//       // sh 'git push origin ${SCALNET_PROJECT}-${RELEASE_VERSION}'
//     }
//   }
// }
