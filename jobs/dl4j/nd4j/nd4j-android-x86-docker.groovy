node("${DOCKER_NODE}") {

step([$class: 'WsCleanup'])

def nd4j = docker.image('deeplearning4j-docker-registry.bintray.io/android:latest')
     nd4j.inside('') {
         sh '''
         #wget https://dl.google.com/android/repository/android-ndk-r13b-linux-x86_64.zip
         #unzip android-ndk-r13b-linux-x86_64.zip
         #export ANDROID_NDK=$(pwd)/android-ndk-r13b
         git clone https://github.com/deeplearning4j/libnd4j
         git clone https://github.com/deeplearning4j/nd4j
         cd libnd4j && git pull && bash buildnativeoperations.sh -platform android-x86
         cd ../nd4j && git pull && mvn clean install -Djavacpp.platform=android-x86 -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
        '''
     }
}
