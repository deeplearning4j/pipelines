appsList = [
    [
        platfrom: "linux-x86_64",
        dockerImage: "deeplearning4j-docker-registry.bintray.io/centos6cuda80:latest",
        dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw  -v ${JENKINS_SBTDIR_AMD64}:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro",
        apps: [
            [
                name: "libnd4j",
                type: "c",
                loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
            ],
            [
                name: "nd4j",
                type: "java",
                loadFile: "${PDIR}/nd4j/nd4j-docker.groovy"
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
        apps: [
            [
                name: "libnd4j",
                type: "c",
                loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
            ],
            [
                name: "nd4j",
                type: "java",
                loadFile: "${PDIR}/nd4j/nd4j-docker.groovy"
            ]
        ]
    ],
    [
        platfrom: "android-arm",
        dockerImage: "deeplearning4j-docker-registry.bintray.io/android:latest",
        dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw",
        apps: [
            [
                name: "libnd4j",
                type: "c",
                loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
            ],
            [
                name: "nd4j",
                type: "java",
                loadFile: "${PDIR}/nd4j/nd4j-android-arm-docker.groovy"
            ]
        ]
    ],
    [
        platfrom: "android-x86",
        dockerImage: "deeplearning4j-docker-registry.bintray.io/android:latest",
        dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw",
        apps: [
            [
                name: "nd4j",
                type: "java",
                loadFile: "${PDIR}/nd4j/nd4j-android-x86-docker.groovy"
            ]
        ]
    ],
    [
        platfrom: "windows",
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
