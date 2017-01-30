timestamps {
  node ('master') {
    step([$class: 'WsCleanup'])
    // def scriptName = this.class.getName()
    // println "Script FQCN : " + scriptName
    // println "Script Simple Name : " + this.class.getSimpleName()
    git url: 'https://github.com/jglick/simple-maven-project-with-tests.git'
    // def mvnHome = tool 'M339'
    // echo "AAA ${mvnHome} BBB"
    // // env.PATH = "${mvnHome}/bin:${env.PATH}"
    // echo "CCC ${env.BUILD_TAG} DDD"
    // sh([script: 'echo $BUILD_TAG'])
    // sh([script: 'mvn -B verify'])
    // sh 'mvn -B verify'
    def gradleHome = tool 'GR33'
    echo "00-${gradleHome}"
    // env.PATH = "${mvnHome}/bin:${env.PATH}"
    echo "01-${env.BUILD_TAG}"
    sh([script: 'echo $BUILD_TAG'])
    sh([script: 'env'])

    // Messages for debugging
    echo 'MARK: end of test_03.groovy'
    step([$class: 'WsCleanup'])
  }
}
