timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${ND4S_PROJECT}") {
            load "${AMD64DIR}/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
        }
    }
}
