  node ('jenkins-slave-cuda') {
    docker.image('8.0cudnn5develcentos6').inside('--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia-uvm-tools --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro') {
            //                 sh '''
            //                 source /opt/rh/devtoolset-3/enable
            //                 ./buildnativeoperations.sh -c cpu
            //                 '''


    stage('Check Maven tool') {
      def mvnHome = tool 'M339'
      echo "${mvnHome}"
      env.PATH = "${mvnHome}/bin:${env.PATH}"
      sh([script: 'mvn -version'])
    }

    stage('Check JDK tool') {
      // tool name: 'JDK8u121', type: 'jdk'
      // def jdkHome = tool 'JDK8u121'
      // sh ("'${jdkHome}/bin/java' -version")

      env.JAVA_HOME="${tool 'JDK8u121'}"
      env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
      sh 'java -version'
    }

    stage('Check SBT tool') {
        tool name: 'SBT100M4', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
        def sbtHome = tool 'SBT100M4'
        sh ("'${sbtHome}/bin/sbt' -help")
    }

    stage('Show some info') {
      echo "From Groovy - ${env.BUILD_TAG}"
      sh([script: 'echo $BUILD_TAG'])
      sh([script: 'env'])
    }

    // Messages for debugging
    echo 'MARK: end of test_jdk.groovy'
    step([$class: 'WsCleanup'])
  }
}
