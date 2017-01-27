timestamps {
    node('master') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load 'jobs/dl4j/rl4j/rl4j.groovy'
        }
    }
}
