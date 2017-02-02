timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load "${PDIR}/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
        }
    }
}
