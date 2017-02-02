timestamps {
    node('master') {

        checkout scm

        stage("${SCALNET_PROJECT}") {
            load 'jobs/dl4j/scalnet/scalnet.groovy'
        }
    }
}
