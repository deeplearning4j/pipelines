timestamps {
    node('master') {

        checkout scm

        stage("${DATAVEC_PROJECT}") {
            load 'jobs/dl4j/datavec/datavec.groovy'
        }
    }
}
