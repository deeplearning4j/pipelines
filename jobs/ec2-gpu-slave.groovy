#!groovy
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
        },
        "stream 3" : {
            node ('ec2cuda') {
                stage("Check CUDA 7.5 with docker and tmpfs") {
                    docker.image('nvidia/cuda:7.5-cudnn5-devel-centos6').inside {
                        sh '( df -h && nvidia-smi )'
                    }
                }
            }
        },
        "stream 4" : {
            node ('ec2cuda') {
                stage("Check CUDA 8 with docker and tmpfs") {
                    docker.image('nvidia/cuda:8.0-cudnn5-devel-centos6').inside {
                        sh '( df -h && nvidia-smi )'
                    }
                }
            }
        }
    )
}
