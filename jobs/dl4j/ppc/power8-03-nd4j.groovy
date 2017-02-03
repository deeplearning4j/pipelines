def nd4j = docker.image('ppc64le/maven:ready')
     nd4j.inside ('-v /mnt/nd4j:/nd4j -v /mnt/libnd4j:/libnd4j'){
         sh '( cd /nd4j && sudo mvn clean install -DskipTests  -Denv.LIBND4J_HOME=/libnd4j )'
    }