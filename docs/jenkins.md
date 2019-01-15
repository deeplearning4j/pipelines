# Jenkins master deployment on Kubernetes cluster

<span style="color:orange">_**For production deployment, please use manifest files with `-prod` suffix.**_</span>

## Prerequisites
1. `kubectl` installed on host.
2. Target Kubernetes cluster endpoint/credentials should be provided in `~/.kube/config` or path to cluster configuration should be provided as env variable.
3. Target Kubernetes cluster should has `citools` agent pool.
4. Jenkins Docker image `jenkins-master-skymind` should be publish to Docker registry.

## Deployment
To deploy new Jenkins master instance on Kubernetes cluster, following set of steps is required:
1. Build fresh Jenkins master Docker image, with following command:

   ```
   cd k8s/ci-skymind/jenkins && \
   docker build -t skymindops/pipelines:jenkins-master-skymind
   ```

   Custom or not supported (plugins that can't be fetched from `Jenkins Update site`) Jenkins plugins located in `k8s/ci-skymind/jenkins/plugins`.
   Currently we do have following custom plugins:

   * *configuration-as-code.hpi*
   * *github-skip-pr-by-title.hpi*

   `cicd-infrastructure/k8s/ci-skymind/jenkins/configs/plugins` folder contains two versions of Jenkins plugins list, that are available at `Jenkins Update site`.
   One of them (`plugins_latest.txt`) has list of all required plugins, but with **latest** version of the plugin.
   Second one (`plugins_locked.txt`), has list of all required plugins, but with **locked** version of the plugin.

   By default, during the baking Docker image for Jenkins master, `plugins_locked.txt` version or required plugins is picked.
   All plugins in the list and custom/not supported plugins are installed during the Jenkins master Docker image build.

2. Create Kubernetes namespaces required for Jenkins operation, with following command:
   `kubectl apply -f k8s/ci-skymind/namespaces/namespaces.yml`.

   Command above will create following namespaces:
   * `ci-tools` - used for grouping all required CI tools;
   * `ci-jenkins-agents` - used for grouping Jenkins build agents.

3. Run `kubectl apply -f k8s/ci-skymind/jenkins/secrets/secrets_prod.yml` to create all required secrets (Jenkins credentials, static agents login/passwords, etc) that will be used by Jenkins.
   Inside the container, secrets are stored as simple text files under `/secrets/jenkins/casc/values`.
   Currently, all *Secret* manifests are stored on administrator development VM.

   <span style="color:orange">_**Kubernetes Secret manifest files should never be committed to git!**_</span>

4. Run `kubectl apply -f k8s/ci-skymind/jenkins/configs/config-prod.yml` to create a Kubernetes `ConfigMap` object to stores whole Jenkins configuration, that will be used by `Jenkins configuration-as-code` plugin, to bring Jenkins to the desired state.
5. Run `kubectl apply -f k8s/ci-skymind/jenkins/deployments/jenkins-prod.yml`.

   `jenkins-prod.yml` manifest file contains following Kubernetes objects:
   * `jenkins-deployer` *ServiceAccount*, *Role*, *RoleBinding* objects are required for running pods by Jenkins. `jenkins-deployer` *Role* contains required permissions to manage pods on Kubernetes cluster. `jenkins-deployer` *RoleBinding* maps permissions declared in *Role* to `jenkins-deployer` *ServiceAccount* that is used by Jenkins to manage the pods;
   * `jenkins-env` *ConfigMap* contains environment variables that are used by Jenkins master instance;
   * `jenkins-master` *StatefulSet* describes Jenkins master instance deployment (required volumes, containers in pod, pod affinity);
   * Two *Service* objects. `jenkins-ui service` provides an endpoint for reaching Jenkins Master pod from outside (via ingress controller). `jenkins-slaves service` used as an endpoint for Jenkins agent connections.

   When a fresh instance of Jenkins master deployed, *Kubernetes Persistent Volume Claim* object should be used for Jenkins home folder.

## Update
To update Jenkins master instance a set of manual steps are required:
1. If changes related to Jenkins master version or Jenkins plugins a new Docker image of Jenkins master should be backed.
2. To apply changes related to Jenkins master Docker image, you need to delete the `jenkins-master-0` *Pod*, which will force Kubernetes to fetch updated image and deploy new *Pod*.
   In case, when changes are related to Kubernetes objects (*StatefulSets*, *ConfigMaps*, *Secrets*, etc) they should be applied with `kubectl apply` command.

   To update Jenkins master configuration new `config-prod.yml` should be applied with `kubectl apply` command and `Apply new configuration` button in `Configuration as Code` section of Jenkins UI should be triggered.

In case of migration Jenkins master instance to new cluster and requirement to keep all jobs history, `Azure disk` should be created from a snapshot of `Kubernetes Persistent Volume Claim`.
`Azure disk` should be created in a new cluster resource group. `jenkins-master StatefulSet` should be updated to user `Azure disk` instead of `Kubernetes Persistent Volume Claim`.

## Testing
To test changes related to Jenkins configuration or runtime environment locally (with minikube, kubeadm, etc), please use same order of deployment commands with manifest files that have **-dev** suffix.

## Issues/Improvements
1. Switch to `helm` deployments.
2. Fix issue, with `jenkins-slaves Service` endpoint for static agent connection. Every time when Jenkins master instance has been redeployed you need to specify new `Tunnel connection through` value for static agents.
3. Add Jenkins master instance monitoring.
4. Providing gpg key should be done manually.
5. Credentials for e-mail server should be provided manually.
6. Mask Passwords - Parameters to automatically mask -> Credentials Parameter flag should be set manually.
7. Some trais for Organization folder has problems (https://github.com/jenkinsci/configuration-as-code-plugin/tree/master/demos/jobs):
    ```
    navTraits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
        strategyId(1)
        trust(class: "org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTraitTrustPermission")
    }
    ```
8. Skip github PR should be installed and configured manually (Problem with docker hub builds).
9. GPG keys (secret and public) should be provided manually.