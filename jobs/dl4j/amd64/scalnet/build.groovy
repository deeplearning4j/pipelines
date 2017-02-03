timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load "${AMD64DIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
        }
    }
}
