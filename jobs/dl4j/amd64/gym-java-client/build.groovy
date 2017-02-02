timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load "jobs/dl4j/amd64/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
        }
    }
}
