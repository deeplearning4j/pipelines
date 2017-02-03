timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${GYM_JAVA_CLIENT_PROJECT}") {
            load "${AMD64DIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}.groovy"
        }
    }
}
