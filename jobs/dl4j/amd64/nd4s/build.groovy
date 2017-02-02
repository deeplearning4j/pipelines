timestamps {
    node('master') {

        checkout scm

        stage("${ND4S_PROJECT}") {
            load "jobs/dl4j/amd64/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
        }
    }
}
