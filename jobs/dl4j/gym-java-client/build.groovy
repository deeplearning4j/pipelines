timestamps {
    node('master') {

        checkout scm

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load 'jobs/dl4j/gym-java-client/gym-java-client.groovy'
        }
    }
}
