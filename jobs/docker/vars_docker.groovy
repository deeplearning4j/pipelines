dockerRegistry = "deeplearning4j-docker-registry.bintray.io"
dockerParamsTest = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"

images = [
    // [
    //     name: "centos6cuda75",
    //     dockerNode: "linux-x86_64",
    //     registry: dockerRegistry,
    //     parentImage: "nvidia/cuda:7.5-cudnn5-devel-centos6"
    // ],
    // [
    //     name: "centos6cuda80",
    //     dockerNode: "linux-x86_64",
    //     registry: dockerRegistry,
    //     parentImage: "nvidia/cuda:8.0-cudnn5-devel-centos6"
    // ],
    // [
    //     name: "android",
    //     dockerNode: "linux-x86_64",
    //     registry: dockerRegistry,
    //     parentImage: "maven:latest"
    // ],
    [
        name: "ubuntu14-ppc64le",
        dockerNode: "linux-ppc64le",
        registry: dockerRegistry,
        parentImage: "ppc64le/ubuntu:14.04"
    ]
]
