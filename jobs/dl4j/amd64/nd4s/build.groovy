timestamps {
    node('master') {

        checkout scm

        stage("${ND4S_PROJECT}") {
            load 'jobs/dl4j/nd4s/nd4s.groovy'
        }
    }
}
