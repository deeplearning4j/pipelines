timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load "${AMD64DIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
        }
    }
}
