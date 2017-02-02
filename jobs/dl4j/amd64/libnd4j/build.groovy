timestamps {
    node('master') {

        checkout scm

        stage("${LIBPROJECT}") {
            load 'jobs/dl4j/libnd4j/libnd4j.groovy'
        }
    }
}
