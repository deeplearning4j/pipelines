node("local-slave") {
    stage("${LIBPROJECT}-BuildCPU") {

        functions.get_project_code("${LIBPROJECT}")

        dir("${LIBPROJECT}") {
            env.TRICK_NVCC = "YES"
            env.LIBND4J_HOME = "${PWD}"
            //sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
            sh("./buildnativeoperations.sh -c cpu ")
        }
        stash name: "cpu", includes: "${LIBPROJECT}/blasbuild"

    }
}
