properties([[$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "25"]],
            [$class: "ParametersDefinitionProperty", parameterDefinitions: [
                    [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                    [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-ppc64le\nmacosx-x86_64\nwindows-x86_64", description: "Build project on architecture"],
                    [$class: "BooleanParameterDefinition", name: "SKIP_TEST", defaultValue: true, description: "Select to run tests during mvn execution"],
                    [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                    [$class: "StringParameterDefinition", name: "STAGE_REPO_ID", defaultValue: "", description: "Staging repository Id"],
                    [$class: "BooleanParameterDefinition", name: "CREATE_TAG", defaultValue: false, description: "Select to create tag for release in git repository"],
                    [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                    [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                    [$class: "ChoiceParameterDefinition", name: "PROFILE_TYPE", choices: "nexus\njfrog\nbintray\nsonatype", description: "Profile type"],
                    [$class: "BooleanParameterDefinition", name: "CBUILD", defaultValue: true, description: "Select to build libnd4j"]
            ]
            ]
])

env.PLATFORM_NAME = env.PLATFORM_NAME ?: "master"
node("master") {
    step([$class: 'WsCleanup'])
}
node(PLATFORM_NAME) {
    currentBuild.displayName = "#${currentBuild.number} ${PLATFORM_NAME}"
    ws(WORKSPACE + "_" + PLATFORM_NAME) {
        step([$class: 'WsCleanup'])

        checkout scm

        load "jobs/dl4j/vars.groovy"
        functions = load "jobs/dl4j/functions.groovy"

        // Remove .git folder from workspace
        functions.rm()

        final appsList = [
                [platform      : "linux-x86_64",
                 dockerImage   : "deeplearning4j-docker-registry.bintray.io/centos6cuda80:latest",
                 dockerParams  : "-v ${WORKSPACE}:${WORKSPACE}:rw -v /srv/jenkins/storage/docker_m2:/home/jenkins/.m2:rw -v /srv/jenkins/storage/docker_ivy2:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro --tmpfs /tmp:size=4g",
                 jenkinsStorage: "/srv/jenkins/storage",
                 apps          : [
                         [name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                         [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"],
                         [name: "datavec", loadFile: "${PDIR}/datavec/datavec-${PLATFORM_NAME}.groovy"],
                         [name: "deeplearning4j", loadFile: "${PDIR}/deeplearning4j/deeplearning4j-${PLATFORM_NAME}.groovy"],
                         [name: "arbiter", loadFile: "${PDIR}/arbiter/arbiter-${PLATFORM_NAME}.groovy"],
                         [name: "nd4s", loadFile: "${PDIR}/nd4s/nd4s-${PLATFORM_NAME}.groovy"],
                         [name: "gym-java-client", loadFile: "${PDIR}/gym-java-client/gym-java-client-${PLATFORM_NAME}.groovy"],
                         [name: "rl4j", loadFile: "${PDIR}/rl4j/rl4j-${PLATFORM_NAME}.groovy"],
                         [name: "scalnet", loadFile: "${PDIR}/scalnet/scalnet-${PLATFORM_NAME}.groovy"]
                 ]
                ],
                [platform      : "linux-ppc64le",
                 dockerImage   : "deeplearning4j-docker-registry.bintray.io/ubuntu14-ppc64le:latest",
                 dockerParams  : "-v ${WORKSPACE}:${WORKSPACE}:rw -v /srv/jenkins/storage/docker_m2:/home/jenkins/.m2:rw",
                 jenkinsStorage: "/srv/jenkins/storage",
                 apps          : [[name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                                  [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"]
                 ]
                ],
                [platform      : "android-arm",
                 dockerImage   : "deeplearning4j-docker-registry.bintray.io/android:latest",
                 dockerParams  : "-v ${WORKSPACE}:${WORKSPACE}:rw -v /srv/jenkins/storage/docker_m2:/home/jenkins/.m2:rw",
                 jenkinsStorage: "/srv/jenkins/storage",
                 apps          : [
                         [name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                         [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"]
                 ]
                ],
                [platform      : "android-x86",
                 dockerImage   : "deeplearning4j-docker-registry.bintray.io/android:latest",
                 dockerParams  : "-v ${WORKSPACE}:${WORKSPACE}:rw -v /srv/jenkins/storage/docker_m2:/home/jenkins/.m2:rw",
                 jenkinsStorage: "/srv/jenkins/storage",
                 apps          : [
                         [name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                         [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"]
                 ]
                ],
                [platform: "macosx-x86_64",
                 apps    : [
                         [name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                         [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"]
                 ]
                ],
                [platform: "windows-x86_64",
                 apps    : [
                         [name: "libnd4j", loadFile: "${PDIR}/libnd4j/libnd4j-${PLATFORM_NAME}.groovy"],
                         [name: "nd4j", loadFile: "${PDIR}/nd4j/nd4j-${PLATFORM_NAME}.groovy"]
                 ]
                ]
        ]

        for (i in appsList) {
            if (PLATFORM_NAME == i.platform) {
                for (app in i.apps) {
                    // echo "building " + app.name + " loading file: " + app.loadFile + " docker params: " + i.dockerParams
                    stage(app.name) {
                        functions.def_docker()
                        // functions.def_docker(i.platform, i.dockerImage, i.dockerParams, i.jenkinsStorage)
                        load app.loadFile
                    }
                }
            }
        }
        echo 'MARK: end of all-multiplatform.groovy'
    }
}
