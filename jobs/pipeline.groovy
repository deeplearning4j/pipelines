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

  stage('ARBITER') {
  	load 'jobs/build-04-arbiter.groovy'
  }

  stage('ND4S') {
  	load 'jobs/build-05-nd4s.groovy'
  }
  
  step([$class: 'WsCleanup'])
}