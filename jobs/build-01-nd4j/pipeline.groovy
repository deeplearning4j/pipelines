timestamps {
    node('master') {

        checkout scm

        stage("${PROJECT}") {
            load 'jobs/build-01-nd4j/build-01-nd4j.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
