env.dockerRegistry = "deeplearning4j-docker-registry.bintray.io"
dockerParamsTest = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
env.dockerImages = ["centos6cuda75","centos6cuda80"]
