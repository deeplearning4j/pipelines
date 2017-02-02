timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load "jobs/dl4j/amd64/${RL4J_PROJECT}/${RL4J_PROJECT}.groovy"
        }
    }
}
