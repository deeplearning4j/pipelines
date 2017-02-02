timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${LIBPROJECT}") {
            load "jobs/dl4j/amd64/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }
    }
}
