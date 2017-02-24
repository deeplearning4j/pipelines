dockerRegistry = "deeplearning4j-docker-registry.bintray.io"
dockerParamsTest = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
// env.dockerImages = ["centos6cuda75","centos6cuda80"]

images = [
    [
        name: "centos6cuda75",
        dockerNode: "linux-x86_64",
        registry: dockerRegistry
    ],
    [
        name: "centos6cuda80",
        dockerNode: "linux-x86_64",
        registry: dockerRegistry
    ],
    [
        name: "ubuntu14-ppc64le",
        dockerNode: "linux-ppc64le",
        registry: dockerRegistry
    ]
]
