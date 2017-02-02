timestamps {
    node('master') {

        checkout scm

        stage("${ARBITER_PROJECT}") {
            load "jobs/dl4j/amd64/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
        }
    }
}
