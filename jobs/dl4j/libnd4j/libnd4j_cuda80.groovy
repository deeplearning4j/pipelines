node("jenkins-slave-cuda8") {
    stage("${LIBPROJECT}-BuildCuda-8.0") {

        functions.get_project_code("${LIBPROJECT}")

        dir("${LIBPROJECT}") {
            env.TRICK_NVCC = "YES"
            env.LIBND4J_HOME = "${PWD}"
            sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
            sh("source /opt/rh/devtoolset-3/enable")
            sh("./buildnativeoperations.sh -c cuda -v 8.0")
        }
        stash name: "cuda80", includes: "${LIBPROJECT}/blasbuild"
    }
}