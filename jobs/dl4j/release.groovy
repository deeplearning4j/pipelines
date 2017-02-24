node("${DOCKER_NODE}") {

    step([$class: 'WsCleanup'])

    checkout scm

    echo "Load variables"
    load "${PDIR}/vars.groovy"

    echo "load functions"
    functions = load "${PDIR}/functions.groovy"

    load "${PDIR}/allcc.groovy
}
