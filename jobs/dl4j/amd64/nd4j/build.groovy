timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${PROJECT}") {
            load "jobs/dl4j/amd64/${PROJECT}/${PROJECT}.groovy"
        }
    }
}
