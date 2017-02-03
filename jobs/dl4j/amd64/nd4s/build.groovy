timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${ND4S_PROJECT}") {
            load "${AMD64DIR}/${ND4S_PROJECT}/${ND4S_PROJECT}.groovy"
        }
    }
}
