timestamps {
  node ('power8') {
	 step([$class: 'WsCleanup'])
     
     def cuda = docker.image('ubuntu_cuda_ready:14.04')
     cuda.inside {
         sh '( cd /libnd4j && sudo ./buildnativeoperations.sh -c cuda -сс 30 )'
    }
     step([$class: 'WsCleanup'])
  }
}
