timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${LIBPROJECT}") {
            load "${AMD64DIR}/${LIBPROJECT}/${LIBPROJECT}.groovy"
        }
    }
}
