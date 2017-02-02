timestamps {
    node('master') {

        checkout scm

        stage("${PROJECT}") {
            load 'jobs/dl4j/nd4j/nd4j-devel.groovy'
        }
    }
}
