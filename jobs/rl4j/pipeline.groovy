timestamps {
    node('master') {

        checkout scm

        stage("${RL4J_PROJECT}") {
            load 'jobs/build-07-rl4j/build-07-rl4j.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
