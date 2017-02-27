node("${DOCKER_NODE}") {
def nd4j = docker.image('maven:ready')
     nd4j.inside {     
         sh ''' 
         export ANDROID_NDK=/android-ndk-r13b
         wget https://dl.google.com/android/repository/android-ndk-r13b-linux-x86_64.zip -P /
         unzip /android-ndk-r13b-linux-x86_64.zip -d /
         git clone https://github.com/deeplearning4j/libnd4j
         git clone https://github.com/deeplearning4j/nd4j
         cd libnd4j && git pull && bash buildnativeoperations.sh -platform android-x86
         cd nd4j && git pull && mvn clean install -Djavacpp.platform=android-x86 -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
        '''
     }}