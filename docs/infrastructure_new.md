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

## Jenkins

## Nexus

## Build agents

## Jenkins pipeline scripts for CI/CD
