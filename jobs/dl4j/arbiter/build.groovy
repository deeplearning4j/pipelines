timestamps {
    node('master') {

        checkout scm

        stage("${ARBITER_PROJECT}") {
            load 'jobs/dl4j/arbiter/arbiter.groovy'
        }
    }
}
