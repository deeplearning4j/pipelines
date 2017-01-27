timestamps {
    node('master') {

        checkout scm

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load 'jobs/build-06-gym-java-client/build-06-gym-java-client.groovy'
        }

        echo 'MARK: end of pipeline.groovy'
    }
}
