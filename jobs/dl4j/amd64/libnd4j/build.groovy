timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${LIBPROJECT}") {
            load "${PDIR}/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }
    }
}
