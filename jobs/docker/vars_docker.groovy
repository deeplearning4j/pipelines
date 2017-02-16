env.PDIR = "jobs/docker"
env.dockerRegistry = "deeplearning4j-docker-registry.bintray.io"

dockerParamsTest = "--device=/dev/nvidiactl --device=/dev/nvidia-uvm --device=/dev/nvidia0 --volume=nvidia_driver_367.57:/usr/local/nvidia:ro"
