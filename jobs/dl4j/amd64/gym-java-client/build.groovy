timestamps {
    node('local-slave') {

        step([$class: 'WsCleanup'])

        checkout scm

        load 'jobs/dl4j/vars.groovy'
        functions = load 'jobs/dl4j/functions.groovy'

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load 'jobs/dl4j/amd64/gym-java-client/gym-java-client.groovy'
        }
    }
}
