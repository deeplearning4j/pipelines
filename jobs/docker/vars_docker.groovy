dockerRegistry = "deeplearning4j-docker-registry.bintray.io"
dockerParamsTest = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_375.26:/usr/local/nvidia:ro"

images = [
    [
        name: "centos6cuda80",
        dockerNode: "linux-x86_64",
        registry: dockerRegistry
    ],
    [
        name: "android",
        dockerNode: "linux-x86_64",
        registry: dockerRegistry
    ],
    [
        name: "ubuntu16-ppc64le",
        dockerNode: "linux-ppc64le",
        registry: dockerRegistry
    ],
    [
        name: "ubuntu16cuda90-ppc64le",
        dockerNode: "linux-ppc64le",
        registry: dockerRegistry
    ]
]
