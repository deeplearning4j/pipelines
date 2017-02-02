timestamps {
    node('master') {

        checkout scm

        stage("${DEEPLEARNING4J_PROJECT}") {
            load "jobs/dl4j/amd64/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
        }
    }
}
