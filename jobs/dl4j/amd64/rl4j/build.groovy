timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load "${AMD64DIR}/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
        }
    }
}
