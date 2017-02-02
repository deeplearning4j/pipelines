timestamps {
    node('g2&&slave') {

        checkout scm

        stage("${PROJECT}") {
            load "${PDIR}/${PROJECT}/${PROJECT}.groovy"
        }
    }
}
