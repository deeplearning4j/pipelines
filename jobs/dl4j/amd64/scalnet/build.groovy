timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load "${AMD64DIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}.groovy"
        }
    }
}
