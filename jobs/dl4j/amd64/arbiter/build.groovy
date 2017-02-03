timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${ARBITER_PROJECT}") {
            load "${AMD64DIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}.groovy"
        }
    }
}
