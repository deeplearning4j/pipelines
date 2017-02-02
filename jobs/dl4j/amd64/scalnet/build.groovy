timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load "${PDIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
        }
    }
}
