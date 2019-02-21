# Sonatype OSS Nexus Repository deployment on Kubernetes cluster

For storing `dl4j-test-resources` build artifacts and caching Maven Central and some other, required for builds repositories, a Nexus Repository manager has been deployed.

## Prerequisites
To be able to deploy new k8s cluster instance following tools should be present on VM:
1. `kubectl` installed on host.
2. Target Kubernetes cluster endpoint/credentials should be provided in `~/.kube/config` or path to cluster configuration should be provided as env variable.
3. Target Kubernetes cluster should has `citools` agent pool.
4. If new Nexus instance should be deployed for `SKIL` k8s cluster, then custom Nexus image (`nexus-master-skil`) should be present on docker registry (docker hub or private one), else generic Nexus docker image if fine to use.

## Deployment
To deploy new Nexus instance on Kubernetes cluster, following set of steps is required:

<span style="color:red">_**For production deployment, please use manifest files with `-prod` suffix. Step 1 only required for SKIL Nexus instance deployemtns (to enable apt repository support)**_</span>
1. Build fresh Nexus Docker image, with following command:

   ```
   cd k8s/ci-skil/nexus && \
   docker build -t skymindops/pipelines:nexus-master-skil
   docker push skymindops/pipelines:nexus-master-skil
   ```

   Custom Nexus docker image will have enabled `apt` repository support.

2. Create Kubernetes namespaces required for Nexus operation, with following command:
   `kubectl apply -f k8s/ci-skymind/namespaces/namespaces.yml`.

   Command above will create following namespaces:
   * `ci-tools` - used for grouping all required CI tools.

3. Run `kubectl apply -f k8s/ci-skymind/nexus/deployments/nexus-prod.yml`.

   `nexus-prod.yml` manifest file contains following Kubernetes objects:
   * `ci-nexus` *StatefulSet* describes Nexus master instance deployment (required volumes, containers in pod, pod affinity);
   * `ci-nexus` service provides an endpoint for reaching Nexus pod from outside (via ingress controller).

   When a fresh instance of Nexus deployed, *Kubernetes Persistent Volume Claim* objects should be used for Nexus home and backup folders.

## Update
To update Nexus instance a set of manual steps are required:
1. If changes related to Nexus version or Nexus extensions a new Docker image of Nexus should be baked.
2. To apply changes related to Jenkins master Docker image, you need to delete the `jenkins-master-0` *Pod*, which will force Kubernetes to fetch updated image and deploy new *Pod*.
   In case, when changes are related to Kubernetes objects (*StatefulSets*, *ConfigMaps*, *Secrets*, etc) they should be applied with `kubectl apply` command.

   To update Jenkins master configuration new `config-prod.yml` should be applied with `kubectl apply` command and `Apply new configuration` button in `Configuration as Code` section of Jenkins UI should be triggered.

In case of migration Jenkins master instance to new cluster and requirement to keep all jobs history, `Azure disk` should be created from a snapshot of `Kubernetes Persistent Volume Claim`.
`Azure disk` should be created in a new cluster resource group. `jenkins-master StatefulSet` should be updated to user `Azure disk` instead of `Kubernetes Persistent Volume Claim`.

## Testing
To test changes related to Jenkins configuration or runtime environment locally (with minikube, kubeadm, etc), please use same order of deployment commands with manifest files that have **-dev** suffix.

## Issues/Improvements
1.