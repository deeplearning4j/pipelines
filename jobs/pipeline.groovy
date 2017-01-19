node ('master') {
  step([$class: 'WsCleanup'])
  checkout scm
  
  stage('ND4J') {
     load 'jobs/build-01-nd4j.groovy'
  }

}