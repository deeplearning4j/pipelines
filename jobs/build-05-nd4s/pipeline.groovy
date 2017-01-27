timestamps {
    node('master') {

        checkout scm

        stage("${ND4S_PROJECT}") {
            load 'jobs/build-05-nd4s/build-05-nd4s.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
