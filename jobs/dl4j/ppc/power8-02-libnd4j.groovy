def cuda = docker.image('ubuntu_cuda_ready:14.04')
  cuda.inside ('-e CCACHE_DIR=/ccache -v /mnt/ccache:/ccache -v /mnt/libnd4j:/libnd4j'){
   sh ''' export PATH=/usr/lib/ccache:$PATH && cd /libnd4j
   sudo ./buildnativeoperations.sh -c cuda -v 8.0 '''
  }