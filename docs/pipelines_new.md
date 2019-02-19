# Jenkins pipeline scripts for Skymind CI/CD

All scripts that are used for CI/CD on Jenkins are enabled on global level (`Jenkins -> Manage Jenkins -> Configure System -> Global Pipeline Libraries`).

Configuring Shared Library on global level allows us to store all scripts and job configurations in one place.

If you need to create a job for new project, you simply need to add `Jenkinsfile` (acts like a marker for Jenkins for tracking the repository that has this file) to the root of the repository and Jenkins will automatically create and configure new job for it.

Scripts are stored in Github, according with `Jenkins Pipeline Shared Library` project structure.

More details regarding configuring and benefits of storing CI/CD pipelines script as Share Library can be found [here](https://jenkins.io/doc/book/pipeline/shared-libraries/).

## Testing new logic in scripts

Default branch (version) of Shared Library is set to `develop`. To change branch (version) for testing changes in CI/CD scripts, developer needs add following line at the begging of the `Jenkinsfile`:

<span style="color:orange">_**skymind in snippet below is the name of shared library defined in Jenkins global config**_</span>

```
@Library('skymind@<branch-name>') _
```

## Library description