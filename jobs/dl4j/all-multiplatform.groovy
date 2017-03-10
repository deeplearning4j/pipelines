properties([
    [$class: "BuildDiscarderProperty",
        strategy: [
            $class: "LogRotator",
            artifactDaysToKeepStr: "",
            artifactNumToKeepStr: "",
            daysToKeepStr: "",
            numToKeepStr: "10"
        ]
    ],
    [$class: "ParametersDefinitionProperty",
        parameterDefinitions: [
            [$class: "StringParameterDefinition",
                name: "VERSION",
                defaultValue: "0.7.3-SNAPSHOT",
                description: "Deeplearning component release version"
            ],
            [$class: "ChoiceParameterDefinition",
                name: "PLATFORM_NAME",
                choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86\nwindows-x86_64",
                description: "Build project on architecture"
            ],
            [$class: "BooleanParameterDefinition",
                name: "TESTS",
                defaultValue: false,
                description: "Select to run tests during mvn execution"
            ],
            [$class: "BooleanParameterDefinition",
                name: "SONAR",
                defaultValue: false,
                description: "Select to check code with SonarQube"
            ],
            [$class: "BooleanParameterDefinition",
                name: "CREATE_TAG",
                defaultValue: false,
                description: "Select to create tag for release in git repository"
            ],
            [$class: "StringParameterDefinition",
                name: "GIT_BRANCHNAME",
                defaultValue: "intropro072-01",
                description: "Default Git branch value"
            ],
            [$class: "CredentialsParameterDefinition",
                name: "GITCREDID",
                required: false,
                defaultValue: "github-private-deeplearning4j-id-1",
                description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"
            ],
            [$class: "ChoiceParameterDefinition",
                name: "PROFILE_TYPE",
                choices: "nexus\njfrog\nbintray\nsonatype",
                description: "Profile type"
            ],
            [$class: "BooleanParameterDefinition",
                name: "CBUILD",
                defaultValue: false,
                description: "Select to build libnd4j"
            ],
        ]
    ]
])

node(PLATFORM_NAME) {

    env.PDIR = "jobs/dl4j"
    load "${PDIR}/vars.groovy"

    def appsList = [
        [
            platfrom: "linux-x86_64",
            dockerImage: "deeplearning4j-docker-registry.bintray.io/centos6cuda80:latest",
            dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw  -v ${JENKINS_SBTDIR_AMD64}:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro",
            jenkinsStorage: "/var/lib/jenkins/storage",
            apps: [
                [
                    name: "libnd4j",
                    type: "c",
                    loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
                ],
                [
                    name: "nd4j",
                    type: "java",
                    loadFile: "${PDIR}/nd4j/nd4j-cc-docker.groovy"
                ],
                [
                    name: "datavec",
                    type: "java",
                    loadFile: "${PDIR}/datavec/datavec-docker.groovy"
                ],
                [
                    name: "deeplearning4j",
                    type: "java",
                    loadFile: "${PDIR}/deeplearning4j/deeplearning4j-docker.groovy"
                ],
                [
                    name: "arbiter",
                    type: "java",
                    loadFile: "${PDIR}/arbiter/arbiter-docker.groovy"
                ],
                [
                    name: "nd4s",
                    type: "scala",
                    loadFile: "${PDIR}/nd4s/nd4s-docker.groovy"
                ],
                [
                    name: "gym-java-client",
                    type: "java",
                    loadFile: "${PDIR}/gym-java-client/gym-java-client-docker.groovy"
                ],
                [
                    name: "rl4j",
                    type: "java",
                    loadFile: "${PDIR}/rl4j/rl4j-docker.groovy"
                ],
                [
                    name: "scalnet",
                    type: "scala",
                    loadFile: "${PDIR}/scalnet/scalnet-docker.groovy"
                ]
            ]
        ],
        [
            platfrom: "linux-ppc64le",
            dockerImage: "deeplearning4j-docker-registry.bintray.io/ubuntu14-ppc64le:latest",
            dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_PPC64LE}:/home/jenkins/.m2:rw",
            jenkinsStorage: "/srv/jenkins/storage",
            apps: [
                [
                    name: "libnd4j",
                    type: "c",
                    loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
                ],
                [
                    name: "nd4j",
                    type: "java",
                    loadFile: "${PDIR}/nd4j/nd4j-cc-docker.groovy"
                ]
            ]
        ],
        [
            platfrom: "android-arm",
            dockerImage: "deeplearning4j-docker-registry.bintray.io/android:latest",
            dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw",
            jenkinsStorage: "/var/lib/jenkins/storage",
            apps: [
                [
                    name: "libnd4j",
                    type: "c",
                    loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
                ],
                [
                    name: "nd4j",
                    type: "java",
                    loadFile: "${PDIR}/nd4j/nd4j-cc-docker.groovy"
                ]
            ]
        ],
        [
            platfrom: "android-x86",
            dockerImage: "deeplearning4j-docker-registry.bintray.io/android:latest",
            dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw",
            jenkinsStorage: "/var/lib/jenkins/storage",
            apps: [
                [
                    name: "libnd4j",
                    type: "c",
                    loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
                ],
                [
                    name: "nd4j",
                    type: "java",
                    loadFile: "${PDIR}/nd4j/nd4j-cc-docker.groovy"
                ]
            ]
        ],
        [
            platfrom: "windows-x86_64",
            apps: [
                [
                    name: "libnd4j",
                    type: "c",
                    loadFile: "${PDIR}/libnd4j/libnd4j-windows.groovy"
                ],
                [
                    name: "nd4j",
                    type: "java",
                    loadFile: "${PDIR}/nd4j/nd4j-windows.groovy"
                ]
            ]
        ]
    ]

    echo "Cleanup WS"
    step([$class: 'WsCleanup'])

    checkout scm

    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Set docker image and parameters for current platform
    // functions.def_docker()

    for (i in appsList) {
        if ( PLATFORM_NAME == i.platfrom ) {
            sh ("mkdir -p ${i.jenkinsStorage}/docker_m2 ${i.jenkinsStorage}/docker_ivy2")
            for (app in i.apps) {
                echo "building " + app.name + " loading file: " + app.loadFile + " docker params: " + i.dockerParams
                stage(app.name) {
                    load app.loadFile
                }
            }
        }
    }




    // stage("${LIBPROJECT}") {
    //     if ( CBUILD.toBoolean() ) {
    //         load "${PDIR}/${LIBPROJECT}/${LIBPROJECT}-docker.groovy"
    //     }
    // }
    //
    // stage("${PROJECT}") {
    //     if ( CBUILD.toBoolean() ) {
    //         load "${PDIR}/${PROJECT}/${PROJECT}-cc-docker.groovy"
    //     }
    //     else {
    //         load "${PDIR}/${PROJECT}/${PROJECT}-docker.groovy"
    //     }
    // }
    //
    // stage("${DATAVEC_PROJECT}") {
    //     load "${PDIR}/${DATAVEC_PROJECT}/${DATAVEC_PROJECT}-docker.groovy"
    // }
    //
    // stage("${DEEPLEARNING4J_PROJECT}") {
    //     load "${PDIR}/${DEEPLEARNING4J_PROJECT}/${DEEPLEARNING4J_PROJECT}-docker.groovy"
    // }
    //
    // stage("${ARBITER_PROJECT}") {
    //     load "${PDIR}/${ARBITER_PROJECT}/${ARBITER_PROJECT}-docker.groovy"
    // }
    //
    // stage("${ND4S_PROJECT}") {
    //     load "${PDIR}/${ND4S_PROJECT}/${ND4S_PROJECT}-docker.groovy"
    // }
    //
    // stage("${GYM_JAVA_CLIENT_PROJECT}") {
    //     load "${PDIR}/${GYM_JAVA_CLIENT_PROJECT}/${GYM_JAVA_CLIENT_PROJECT}-docker.groovy"
    // }
    //
    // stage("${RL4J_PROJECT}") {
    //     load "${PDIR}/${RL4J_PROJECT}/${RL4J_PROJECT}-docker.groovy"
    // }
    //
    // // depends on nd4j and deeplearning4j-core
    // stage("${SCALNET_PROJECT}") {
    //     load "${PDIR}/${SCALNET_PROJECT}/${SCALNET_PROJECT}-docker.groovy"
    // }
    //
    //
    // stage('RELEASE') {
    //
    //     // def isSnapshot = VERSION.endsWith('SNAPSHOT')
    //
    //     if (isSnapshot) {
    //         echo "End of building and publishing of the ${VERSION}"
    //     } else {
    //         // timeout(time:1, unit:'HOURS') {
    //         timeout(20) {
    //             input message: "Approve release of version ${VERSION} ?"
    //         }
    //
    //         if ( CBUILD.toBoolean() ) {
    //             functions.release("${LIBPROJECT}")
    //         }
    //
    //         functions.release("${PROJECT}")
    //         functions.release("${DATAVEC_PROJECT}")
    //         functions.release("${DEEPLEARNING4J_PROJECT}")
    //         functions.release("${ARBITER_PROJECT}")
    //         functions.release("${ND4S_PROJECT}")
    //         functions.release("${GYM_JAVA_CLIENT_PROJECT}")
    //         functions.release("${RL4J_PROJECT}")
    //         functions.release("${SCALNET_PROJECT}")
    //
    //     }
    //
    // }

    echo 'MARK: end of all-multiplatfrom.groovy'
}
