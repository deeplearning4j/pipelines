final appsList = [
    [
        platfrom: "linux-x86_64",
		dockerParams: "-v ${WORKSPACE}:${WORKSPACE}:rw -v ${JENKINS_M2DIR_AMD64}:/home/jenkins/.m2:rw  -v ${JENKINS_SBTDIR_AMD64}:/home/jenkins/.ivy2:rw --device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro",
        apps: [
            [
                name: "libnd4j",
                type: "c",
                loadFile: "${PDIR}/libnd4j/libnd4j-docker.groovy"
            ],
            [
                name: "app2",
                type: "java",

            ]
        ]
    ],
    [
        platfrom: "linux-ppc64le",
        apps: [
            [
                name: "app11",
                type: "cpp",
                dockerParams: "params_ppc"

            ],
            [
                name: "app22",
                type: "java",
                dockerParams: "params_ppc"
            ]
        ]
    ]
]
