timestamps {
    node('amd64&&g2&&ubuntu16') {

        checkout scm

        stage("${PROJECT}") {
            load "${AMD64DIR}/${PROJECT}/${PROJECT}.groovy"
        }
    }
}
