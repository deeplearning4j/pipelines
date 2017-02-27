def nd4j = docker.image('maven:latest')
     nd4j.inside {     
         sh ''' 
         export ANDROID_NDK=/android-ndk-r13b
         cd /libnd4j && git pull && bash buildnativeoperations.sh -platform android-arm
         cd /nd4j && mvn clean install -Djavacpp.platform=android-arm -DskipTests -pl \'!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform\'
        '''
     }