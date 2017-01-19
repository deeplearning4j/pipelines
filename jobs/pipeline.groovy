node ('master') {
  step([$class: 'WsCleanup'])
  checkout scm
  
  stage('ND4J') {
    load 'jobs/build-01-nd4j.groovy'
  }

  stage('DATAVEC') {
    load 'jobs/build-02-datavec.groovy'
  }

  stage('DEEPLEARNING4J') {
  	load  'jobs/build-03-deeplearning4j.groovy'
  }
  
  step([$class: 'WsCleanup'])
}