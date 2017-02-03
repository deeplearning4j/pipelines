timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${ARBITER_PROJECT}") {
            load "${AMD64DIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
        }
    }
}
