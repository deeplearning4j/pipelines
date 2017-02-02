timestamps {
    node('master') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load "jobs/dl4j/amd64/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
        }
    }
}
