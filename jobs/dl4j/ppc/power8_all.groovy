timestamps {
    node ('power8') {
    
        step([$class: 'WsCleanup'])
    
        checkout scm      

        parallel (
         "build_cuda_7" : { stage("libnd4j_1") {
          load 'jobs/dl4j/ppc/power8-01-libnd4j.groovy'
         } },

         "build_cuda_8" : { stage("libnd4j_2") {
          load 'jobs/dl4j/ppc/power8-02-libnd4j.groovy'
         } }         
        )
        
         stage("nd4j") {
          load 'jobs/dl4j/ppc/power8-03-nd4j.groovy'
         }
        

        step([$class: 'WsCleanup'])

    }
}
