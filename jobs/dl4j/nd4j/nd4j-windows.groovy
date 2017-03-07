echo "THIS IS WORKS?"

# export WORKSPACE=`pwd`
# echo $WORKSPACE
# export LIBND4J_HOME=${WORKSPACE}/libnd4j
# export PROFILE_TYPE="nexus"
# alias mvn="mvn -s ../../../../intropro-work/Skymind/DO-192/settings.xml"

# git clone -b intropro072-01 git@github.com:deeplearning4j/nd4j.git
# cd nd4j/
# mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=0.7.4-SNAPSHOT

# ./change-cuda-versions.sh 7.5
# ./change-scala-versions.sh 2.10
# mvn clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -Dmaven.test.skip=true -DstagingRepositoryId=${STAGE_REPO_ID}

# ./change-cuda-versions.sh 8.0
# ./change-scala-versions.sh 2.11
# mvn clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -Dmaven.test.skip=true -DstagingRepositoryId=${STAGE_REPO_ID}
