timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load "${PDIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
        }
    }
}
