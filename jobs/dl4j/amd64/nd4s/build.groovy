timestamps {
    node('local-slave') {

        step([$class: 'WsCleanup'])

        checkout scm

        load 'jobs/dl4j/vars.groovy'
        functions = load 'jobs/dl4j/functions.groovy'

        stage("${ND4S_PROJECT}") {
            load 'jobs/dl4j/nd4s/nd4s.groovy'
        }
    }
}
