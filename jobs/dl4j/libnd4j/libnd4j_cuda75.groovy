node("jenkins-slave-cuda7") {
    stage("${LIBPROJECT}-BuildCuda-7.5") {

        functions.get_project_code("${LIBPROJECT}")

        dir("${LIBPROJECT}") {
            env.TRICK_NVCC = "YES"
            env.LIBND4J_HOME = "${PWD}"
            sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
            sh("source /opt/rh/devtoolset-3/enable")
            sh("./buildnativeoperations.sh -c cuda -v 7.5")
        }
        stash name: "cuda75", includes: "${LIBPROJECT}/blasbuild"
    }
}
