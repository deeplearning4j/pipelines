node("master") {

    stage("BuildBaseLibs") {
        parallel (
            "Stream 0 x86_64" : {
                build job: 'devel/dl4j/amd64/libnd4j', parameters:
                    [[$class: 'StringParameterValue', name: 'param1', value:'test_param'],
                    [$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "linux-x86_64"],
                    [$class: 'StringParameterValue', name:'DOCKER_NODE', value: "linux-x86_64"]]
            },
            "Stream 1 ppc64le" : {
                build job: 'devel/dl4j/ppc/libnd4j', parameters:
                    [[$class: 'StringParameterValue', name: 'param1', value: 'test_param'],
                    [$class: 'StringParameterValue', name:'PLATFORM_NAME', value: "ppc64le"],
                    [$class: 'StringParameterValue', name:'DOCKER_NODE', value: "ppc64le"]]
            }
        )
    }

    stage("Stage2"){
        echo "Ok"
    }
    // branches["branch${i}"] = {
    //     build job: 'test_jobs', parameters: [[$class: 'StringParameterValue', name: 'param1', value:
    //      'test_param'], [$class: 'StringParameterValue', name:'dummy', value: "${index}"]]
    // }
    // parallel branches
}
