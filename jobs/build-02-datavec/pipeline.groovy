timestamps {
    node('master') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load 'jobs/build-02-datavec/build-02-datavec.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
