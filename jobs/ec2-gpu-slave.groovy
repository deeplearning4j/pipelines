node {
    node ('ec2cuda') {
        stage ("Provisioning") {
            checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, userRemoteConfigs: [[credentialsId: 'github-private-deeplearning4j-id-1', url: 'git@github.com:deeplearning4j/pipelines.git']]])
            ansiblePlaybook installation: 'ansible_centos(AmazonLinux)', playbook: 'ansible/aws/cd/provision.yml', sudoUser: null
        }
    }
    parallel (
        "stream 1" : {
            node ('ec2cuda') {
                stage("Check nvidia-docker with CUDA 7.5") {
                    def cuda = docker.image('nvidia/cuda:7.5-cudnn5-devel-centos6')
                    cuda.inside {
                        sh '( nvcc --version )'
                    }
                }
            }
        },
        "stream 2" : {
            node ('ec2cuda') {
                stage("Check nvidia-docker with CUDA 8") {
                    def cuda = docker.image('nvidia/cuda:8.0-cudnn5-devel-centos6')
                    cuda.inside {
                        sh '( nvcc --version )'
                    }
                }
            }
        }
    )
}