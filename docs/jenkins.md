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

4. Run `kubectl apply -f k8s/ci-skymind/jenkins/configs/config-prod.yml` to create a Kubernetes `ConfigMap` object to stores whole Jenkins configuration, that will be used by `Jenkins configuration-as-code` plugin, to bring Jenkins to the desired state.
5. Run `kubectl apply -f k8s/ci-skymind/jenkins/deployments/jenkins-prod.yml`.

   `jenkins-prod.yml` manifest file contains following Kubernetes objects:
   * `jenkins-deployer` *ServiceAccount*, *Role*, *RoleBinding* objects are required for running pods by Jenkins. `jenkins-deployer` *Role* contains required permissions to manage pods on Kubernetes cluster. `jenkins-deployer` *RoleBinding* maps permissions declared in *Role* to `jenkins-deployer` *ServiceAccount* that is used by Jenkins to manage the pods;
   * `jenkins-env` *ConfigMap* contains environment variables that are used by Jenkins master instance;
   * `jenkins-master` *StatefulSet* describes Jenkins master instance deployment (required volumes, containers in pod, pod affinity);
   * Two *Service* objects. `jenkins-ui service` provides an endpoint for reaching Jenkins Master pod from outside (via ingress controller). `jenkins-slaves service` used as an endpoint for Jenkins agent connections.

   TODO: describe storage pitfalls
   TODO: describe where to store secrets

## Update

## Testing

## Issues/Improvements