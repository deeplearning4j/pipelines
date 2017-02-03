timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${DEEPLEARNING4J_PROJECT}") {
            load "${AMD64DIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}.groovy"
        }
    }
}
