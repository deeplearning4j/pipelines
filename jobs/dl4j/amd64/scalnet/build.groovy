timestamps {
    node('master') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load "jobs/dl4j/amd64/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
        }
    }
}
