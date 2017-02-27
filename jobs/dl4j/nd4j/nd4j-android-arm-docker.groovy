node("${DOCKER_NODE}") {

step([$class: 'WsCleanup'])
   
def nd4j = docker.image('maven:ready')
     nd4j.inside('') {     
         sh '''          
         wget https://dl.google.com/android/repository/android-ndk-r13b-linux-x86_64.zip
         unzip android-ndk-r13b-linux-x86_64.zip
         git clone https://github.com/deeplearning4j/libnd4j
         git clone https://github.com/deeplearning4j/nd4j
         export ANDROID_NDK=./android-ndk-r13b
         cd libnd4j && git pull && bash buildnativeoperations.sh -platform android-arm
         cd nd4j && git pull && mvn clean install -Djavacpp.platform=android-arm -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
        '''
     } }