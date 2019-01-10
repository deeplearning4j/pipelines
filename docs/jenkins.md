# Jenkins master deployment on Kubernetes cluster

<span style="color:orange">_**For production deployment, please use manifest files with `_prod` sufifx.**_</span>

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

   * configuration-as-code.hpi
   * github-skip-pr-by-title.hpi

   These plugins are installed during the Jenkins master Docker image build.


   TODO: describe plugins folder!

2. Create Kubernetes namespaces required for Jenkins operation, with following command:
   `kubectl apply -f k8s/ci-skymind/namespaces/namespaces.yml`.

   Command above will create following namespaces:
   * `ci-tools` - used for grouping all required CI tools;
   * `ci-jenkins-agents` - used for grouping Jenkins build agents.

3. Run `kubectl apply -f k8s/ci-skymind/jenkins/secrets/secrets_prod.yml` to create all required secrets (Jenkins credentials, static agents login/passwords, etc) that will be used by Jenkins.
4. Run `kubectl apply -f k8s/ci-skymind/jenkins/configs/config-prod.yml` to create a Kubernetes `ConfigMap` object to stores whole Jenkins configuration, that will be used by `Jenkins configuration-as-code` plugin, to bring Jenkins to the desired state.
5. Run `kubectl apply -f k8s/ci-skymind/jenkins/deployments/jenkins-prod.yml`

   TODO: describe all objects in jenkins-prod manifest
   TODO: describe storage pitfalls
   TODO: describe where to store secrets

## Update

## Testing

## Issues/Improvements