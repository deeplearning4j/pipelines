timestamps {
    node('master') {

        checkout scm

        stage("${LIBPROJECT}") {
            load "jobs/dl4j/amd64/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }
    }
}
