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
   * `ci-nexus` *Service* provides an endpoint for reaching Nexus pod from outside (via ingress controller).

   When a fresh instance of Nexus deployed, *Kubernetes Persistent Volume Claim* objects should be used for Nexus home and backup folders.

## Update
To update Nexus instance a set of manual steps are required:
1. If changes related to Nexus version or Nexus extensions a new Docker image of Nexus should be baked.
2. To apply changes related to Nexus Docker image, you need to delete the `ci-nexus-0` *Pod*, which will force Kubernetes to fetch updated image and deploy new *Pod*.
   In case, when changes are related to Kubernetes objects (*StatefulSets*, *ConfigMaps*, *Secrets*, etc) they should be applied with `kubectl apply` command.

In case of migration Nexus instance to new cluster and requirement to keep all jobs history, `Azure disk` should be created from a snapshot of `Kubernetes Persistent Volume Claim`.
`Azure disk` should be created in a new cluster resource group. `ci-nexus` *StatefulSet* should be updated to user `Azure disk` instead of `Kubernetes Persistent Volume Claim`.

### Increase volume size
<span style="color:red">_**Please note, at the moment Azure allows only increase the volume size, decrease is not allowed.**_</span>

To increase the volume size of existing Nexus deployment, following set of steps are required:
1. Delete `ci-nexus` *StatefulSet*, to release attached to `ci-nexus-0` *Pod* volume.
2. Increase volume size, either from `Azure UI` or `Azure CLI`.
3. Delete `nexus-data-volume-ci-nexus-0` *Persistent Volume Claim*, which will trigger allocated (old) *Persistent Volume* removal.
4. Redeploy Nexus `ci-nexus` *StatefulSet*, all settings will be lost at this point (you should user default user and password).
5. Restore Nexus configuration from backup, mode details can be found at [nexus restore from backup docs](https://help.sonatype.com/repomanager3/backup-and-restore/restore-exported-databases).

## Testing
To test changes related to Nexus configuration or runtime environment locally (with minikube, kubeadm, etc), please use same order of deployment commands with manifest files that have **-dev** suffix.

## Issues/Improvements
1. Missing base Nexus configuration in git, only daily backups are stored on separate volume.
2. Add monitoring of Nexus instance and uploads/downloads.
3. Add artifacts security scan.
4. Add scaling of read only Nexus nodes.
5. Combine all Nexus instances into one (SKIL, CI, SKIL CI).