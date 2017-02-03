timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load "${AMD64DIR}/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
        }
    }
}
