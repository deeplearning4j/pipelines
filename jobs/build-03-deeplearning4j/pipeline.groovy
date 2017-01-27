timestamps {
    node('master') {

        checkout scm

        stage("${DEEPLEARNING4J_PROJECT}") {
            load 'jobs/build-03-deeplearning4j/build-03-deeplearning4j.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
