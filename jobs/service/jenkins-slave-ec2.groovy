node {
    step([$class: 'WsCleanup'])
    node ("${env.LABEL}") {
        stage ("Create jenkins user") {
            git branch: 'devel', credentialsId: 'github-private-deeplearning4j-id-1', url: 'git@github.com:deeplearning4j/pipelines.git'
            ansiblePlaybook installation: 'ansible_centos(AmazonLinux)', playbook: 'ansible/aws/cd/user_jenkins.yml', sudoUser: null
            stash includes: 'ansible/aws/cd/IP', name: 'IP_slave' 
            sh 'sudo shutdown -r 1 &'
        }
    }
    stage ("Change UID for main user") {
        unstash 'IP_slave'
        sh "cat $WORKSPACE/ansible/aws/cd/IP > result";
        def IP=readFile('result')
        git branch: 'devel', credentialsId: 'github-private-deeplearning4j-id-1', url: 'git@github.com:deeplearning4j/pipelines.git'
        sleep 60
        timeout(2) {
            waitUntil {
                def r = sh script: "ssh -i /tmp/cd-jenkins.pem -q -oStrictHostKeyChecking=no jenkins@${IP} exit", returnStatus: true
                return (r == 0);
            }
        }
        ansiblePlaybook extras: "--private-key /tmp/cd-jenkins.pem", inventory: "${IP},", installation: 'ansible-local', playbook: 'ansible/aws/cd/change-uid.yml'
        step([$class: 'WsCleanup'])
    }
    node ("${env.LABEL}") {
        stage ("Provisioning") {
            git branch: 'devel', credentialsId: 'github-private-deeplearning4j-id-1', url: 'git@github.com:deeplearning4j/pipelines.git'
            ansiblePlaybook installation: 'ansible_centos(AmazonLinux)', playbook: 'ansible/aws/cd/provision.yml', sudoUser: null
        }
    }
    step([$class: 'WsCleanup'])
}