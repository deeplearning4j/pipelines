timestamps {
    node('master') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load 'jobs/build-08-scalnet/build-08-scalnet.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
