node("${DOCKER_NODE}") {
def nd4j = docker.image('maven:latest')
     nd4j.inside {     
         sh ''' 
         export ANDROID_NDK=/android-ndk-r13b
         whoami
         cd /libnd4j && git pull && bash buildnativeoperations.sh -platform android-x86
         cd /nd4j && git pull && mvn clean install -Djavacpp.platform=android-x86 -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
        '''
     }}