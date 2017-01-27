timestamps {
    node('master') {

        checkout scm

        stage("${DEEPLEARNING4J_PROJECT}") {
            load 'jobs/dl4j/deeplearning4j/deeplearning4j.groovy'
        }
    }
}
