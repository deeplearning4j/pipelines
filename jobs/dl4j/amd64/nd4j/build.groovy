timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${PROJECT}") {
            load "${AMD64DIR}/${PROJECT}/${PROJECT}.groovy"
        }
    }
}
