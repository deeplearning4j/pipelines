#!groovy

node('local-slave') {
    step([$class: 'WsCleanup'])

    stage('PrepareAWSTool') {
        sh'''
        docker pull python:2-slim
        export JENKINSUID=`id -u`
        export JENKINSGID=`id -g`
        mkdir -p /tmp/awscli
        echo "FROM python:2-slim" > /tmp/awscli/Dockerfile
        echo "RUN pip install awscli" >> /tmp/awscli/Dockerfile
        echo "RUN groupadd jenkins -g $JENKINSGID && useradd -u $JENKINSUID -g $JENKINSGID -m jenkins" | tee -a /tmp/awscli/Dockerfile
        cat /tmp/awscli/Dockerfile
        '''
        docker.build ('awscli','/tmp/awscli')
        sh "rm -f /tmp/awscli/Dockerfile"
    }
    stage('ShowRunningEC2Instances') {
        checkout([$class: 'GitSCM',
        branches: [[name: '*/docker-slaves']],
        doGenerateSubmoduleConfigurations: false,
        extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '.'], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
        submoduleCfg: [],
        userRemoteConfigs: [[url: 'git@github.com:intropro/skymind-cd.git', credentialsId: 'github-private-intropro-skymind-cd-id-1']]])
        dir('docker') {
            docker.image('awscli').inside {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                                credentialsId: 'aws-ec2-ceredntials-id-1',
                                secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    withEnv(['AWS_DEFAULT_REGION=us-west-2']) {
                        print "Instances Name=tag:DeployedBy,Values=JenkinsEc2Plugin:"
                        sh '( aws ec2 describe-instances --filters "Name=tag:DeployedBy,Values=JenkinsEc2Plugin,JenkinsWithTerraform" "Name=instance-state-name,Values=pending,running,shutting-down,stopping,stopped" )'
                        // print "All running instances:"
                        // sh 'aws ec2 describe-instances --filters "Name=instance-state-name,Values=running"'
                        // sh 'aws ec2 stop-instances --instance-ids "i-07bd4875872e0d259" "i-07af07b37fe2772b7"'
                        // sh 'aws ec2 terminate-instances --instance-ids "i-07bd4875872e0d259" "i-07af07b37fe2772b7"'
                    }
                }
            }
        }
    }
}
