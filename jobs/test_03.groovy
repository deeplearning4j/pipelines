timestamps {
  node ('master') {
    step([$class: 'WsCleanup'])
    // def scriptName = this.class.getName()
    // println "Script FQCN : " + scriptName
    // println "Script Simple Name : " + this.class.getSimpleName()
    git url: 'https://github.com/jglick/simple-maven-project-with-tests.git'
    def mvnHome = tool 'M339'
    echo "AAA ${mvnHome} BBB"
    // env.PATH = "${mvnHome}/bin:${env.PATH}"
    echo "CCC ${env.BUILD_TAG} DDD"
    sh([script: 'echo $BUILD_TAG'])
    // sh([script: 'mvn -B verify'])
    // sh 'mvn -B verify'

    // Messages for debugging
    echo 'MARK: end of test.groovy'
    step([$class: 'WsCleanup'])
  }
}
