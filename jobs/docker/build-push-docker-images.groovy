def strToList(str) {
    if (str.getClass() == String && str.length()>0) {
        tmpList = []
        for ( i in str.split(",")) {
            def item = i
            tmpList.add(item);
        }
        } else {
            error "strToList(): Input arg isn't string or empty, class: ${str.getClass()}, size: ${str.length()}"
        }
        return tmpList
    }

properties([
           [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
           [$class: "ParametersDefinitionProperty", parameterDefinitions:
                   [
                           [$class: "LabelParameterDefinition", name: "DOCKER_NODE", defaultValue: "jenkins-slave-cuda", description: "Correct parameters:\njenkins-slave-cuda\nsshlocal\npower8\nppc"],
                           [$class: "StringParameterDefinition", name: "PDIR", defaultValue: "jobs/dl4j", description: "Path to groovy scripts"],
                           [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\njfrog\nbintray\nsonatype", description: "Profile type"]
                   ]
           ]
   ])

   <parameterDefinitions>
           <org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition plugin="nodelabelparameter@1.7.2">
             <name>DOCKER_NODE</name>
             <description>Choose node to build and store images</description>
             <defaultValue>jenkins-slave-cuda</defaultValue>
             <allNodesMatchingLabel>false</allNodesMatchingLabel>
             <triggerIfResult>allCases</triggerIfResult>
             <nodeEligibility class="org.jvnet.jenkins.plugins.nodelabelparameter.node.AllNodeEligibility"/>
           </org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition>
           <com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition plugin="extended-choice-parameter@0.75">
             <name>DOCKER_IMAGES</name>
             <description>Check images to build</description>
             <quoteValue>false</quoteValue>
             <saveJSONParameterToFile>false</saveJSONParameterToFile>
             <visibleItemCount>4</visibleItemCount>
             <type>PT_CHECKBOX</type>
             <value>centos6cuda80,centos6cuda75,ubuntu14cuda80,ubuntu14cuda75</value>
             <defaultValue>centos6cuda80,centos6cuda75</defaultValue>
             <multiSelectDelimiter>,</multiSelectDelimiter>
           </com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition>
           <hudson.model.BooleanParameterDefinition>
             <name>PUSH_TO_REGISTRY</name>
             <description>Check to push to docker registry deeplearning4j-docker-registry.bintray.io</description>
             <defaultValue>false</defaultValue>
           </hudson.model.BooleanParameterDefinition>
         </parameterDefinitions>

node("${DOCKER_NODE}") {

    stage ('Checkout') {
        checkout scm
    }

    echo "Load variables"
    load "jobs/docker/vars_docker.groovy"

    // def images = ['centos6cuda80', 'centos6cuda75']
    def builders = [:]
    if ( DOCKER_IMAGES.size() > 0 ) {
        images = strToList(DOCKER_IMAGES)
    }
    for (i in images) {
        def index = i
        builders[index] = {
            stage ("Build ${index}") {
                docker.build ("${dockerRegistry}/${index}","docker/${index}")
            }
            stage ("Test ${index}") {
                docker.image("${dockerRegistry}/${index}").inside(dockerParamsTest) {
                    sh '''
                    java -version
                    mvn -version
                    /opt/sbt/bin/sbt sbt-version
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                    cmake --version
                    gcc -v
                    '''
                }
            }
            stage ("Push ${index}") {
                if ( PUSH_TO_REGISTRY.toBoolean() ) {
                    withDockerRegistry([credentialsId: 'BintrayDockerRegistry', url: 'https://${dockerRegistry}']) {
                        docker.image("${dockerRegistry}/${index}").push 'latest'
                    }
                } else {
                    echo "Skipping push to registry"
                }
            }
        }
    }
    parallel builders
}
