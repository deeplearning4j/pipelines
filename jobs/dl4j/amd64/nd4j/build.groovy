timestamps {
    node('master') {

        checkout scm

        stage("${PROJECT}") {
            load "jobs/dl4j/amd64/${PROJECT}/${PROJECT}.groovy"
        }
    }
}
