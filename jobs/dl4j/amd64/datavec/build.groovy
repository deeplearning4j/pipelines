timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
        }
    }
}
