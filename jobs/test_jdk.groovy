timestamps {
  node ('jenkins-slave-cuda') {
    step([$class: 'WsCleanup'])
    git url: 'https://github.com/jglick/simple-maven-project-with-tests.git'
    def mvnHome = tool 'M339'
    echo "AAA ${mvnHome} BBB"
    // env.PATH = "${mvnHome}/bin:${env.PATH}"
    echo "CCC ${env.BUILD_TAG} DDD"
    sh([script: 'echo $BUILD_TAG'])
    sh([script: 'mvn -version'])
    sh 'mvn -B verify'
    env.JAVA_HOME="${tool 'JDK8u121'}"
    env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    sh 'java -version'
    // echo "01-${env.BUILD_TAG}"
    // sh([script: 'echo $BUILD_TAG'])
    sh([script: 'env'])

    // Messages for debugging
    echo 'MARK: end of test_jdk.groovy'
    step([$class: 'WsCleanup'])
  }
}
