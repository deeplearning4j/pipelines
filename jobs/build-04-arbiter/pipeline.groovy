timestamps {
    node('master') {

        checkout scm

        stage("${ARBITER_PROJECT}") {
            load 'jobs/build-04-arbite/build-04-arbiter.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
