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
  
  stage('GYM-JAVA-CLIENT') {
  	load 'jobs/build-06-gym-java-client.groovy'
  }

  stage('RL4J') {
  	load 'jobs/build-07-rl4j.groovy'
  }

  step([$class: 'WsCleanup'])
}