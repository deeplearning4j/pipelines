timestamps {
    node('local-slave') {

        step([$class: 'WsCleanup'])

        checkout scm

        load 'jobs/dl4j/vars.groovy'
        functions = load 'jobs/dl4j/functions.groovy'

        stage("${ARBITER_PROJECT}") {
            load 'jobs/dl4j/arbiter/arbiter.groovy'
        }
    }
}
