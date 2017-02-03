timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${DEEPLEARNING4J_PROJECT}") {
            load "${AMD64DIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
        }
    }
}
