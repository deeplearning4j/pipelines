# Description of infrastructure/tools/scripts for CI/CD of Deeplearning4j organization projects

## Brief description of current infrastructure
Currently, our CI/CD infrastructure consists of private k8s cluster that hosts all required CI/CD tools, and build agents.

Also, we do have:
* `macOS boxes`, hosted at [Macminivault](https://www.macminivault.com);
* `PPC64LE boxes`, provided by [OSU Open Source Lab](https://oregonstate.edu/);
* `Windows box`, deployed on Azure, in the same private network as kubernetes cluster;
* `Linux(GPU) box` deployed on Azure, in the same private network as kubernetes cluster.

`macOS` and `PPC64LE(CPU)` boxes connected to Jenkins instance via `ssh`,
whereas `PPC64LE(GPU)` is using Jenkins `SGE plugin` to create and connect Jenkins `PPC64LE(GPU)` agents.

Below, you can find general view of all CI/CD tools that are currently in use ([Pic.1](#pic1---general-view-of-cicd-infrastructure)).

![Pic.1 - General view of CI/CD infrastructure](imgs/ci_cd_infrastructure.png)

###### Pic.1 - General view of CI/CD infrastructure

## Kubernetes
Kubernetes cluster for CI/CD infrastructure was deployed on Azure with the help of [ACS engine](https://github.com/Azure/acs-engine).

Azure resource group name for deployed cluster: `ci-skymind-prod-acs-cluster-03`.

|Name|Version|
|----|-------|
|ACS engine|v0.26.2|
|Kubernetes|v1.11.5|

Private Kubernetes cluster ACS engine template was used for current deployment, it can be found [here](cicd-infrastructure/azure/acs/ci-skymind-cluster/acs-engine-v0.26.2/templates/ci-kubernetes-hybrid-cluster-with-jumpbox-linux-pools-only.json).

Cluster has following agent pools:

|Pool name|Azure instance type|Pool description|
|:---------:|:-------------------:|----------------|
|citools|Standard_D4s_v3|Used for CI tools deployment. Currently contains `Jenkins` and `OSS Nexus` instances|
|linuxcpu1|Standard_D4_v3|Used for most of the `Jenkins` build agents.|
|linuxcpu2|Standard_F8s_v2|Used only for `Jenkins` linux `avx512` build agents.|

## Jenkins

## Nexus

## Build agents

## Jenkins pipeline scripts for CI/CD
