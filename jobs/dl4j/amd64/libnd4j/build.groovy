timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${LIBPROJECT}") {
            load "${AMD64DIR}/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }
    }
}
