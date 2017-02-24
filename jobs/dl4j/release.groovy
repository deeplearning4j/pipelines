node {

    step([$class: 'WsCleanup'])

    checkout scm
    stash includes: 'jobs/', name: 'jobs'

    echo "Load variables"
    load "${PDIR}/vars.groovy"

    echo "load functions"
    functions = load "${PDIR}/functions.groovy"

    parallel (
        "Stream 0" : {
            node("linux-x86_64") {
                echo "Build on linux-x86_64"
                unstash 'jobs'
                def PLATFORM_NAME = "linux-x86_64"
                // env.PLATFORM_NAME = "linux-x86_64"
                sh("ls -1 && env")
                load "${PDIR}/allcc.groovy"
            }
        },
        "Stream 1" : {
            node("linux-ppc64le"){
                echo "Build on linux-ppc64le"
                unstash 'jobs'
                // env.PLATFORM_NAME = "linux-ppc64le"
                def PLATFORM_NAME = "linux-ppc64le"
                sh("ls -1 && env")
                load "${PDIR}/allcc.groovy"
            }
        }
    )
}
