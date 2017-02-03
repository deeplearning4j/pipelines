timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load "${AMD64DIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}.groovy"
        }
    }
}
