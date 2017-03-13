stage("${LIBPROJECT}-checkout-sources") {

    functions.get_project_code("${LIBPROJECT}")
    dir("${LIBPROJECT}") {
        functions.checktag("${LIBPROJECT}")
    }

    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "javacpp"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[url: "https://github.com/bytedeco/javacpp.git"]]
    ])
}

stage("build javacpp") {
  configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
    switch (PLATFORM_NAME) {
        case "macosx":
            dir('javacpp') {
              sh '''
              env && mvn -B clean install -DskipTests -Dmaven.javadoc.skip=true
              '''
            }

          break
        case "windows-x86_64":
            dir('javacpp') {
              bat (
                  'set' +
                  '&&' +
                  'vcvars64.bat' +
                  '&&' +
                  'mvn -B -s %MAVEN_SETTINGS% clean install -DskipTests -Dmaven.javadoc.skip=true'
              )
            }

          break

        default:
          error("Platform name - ${PLATFORM_NAME} is not defined or unsupported")

          break
    }
  }
}


stage("${LIBPROJECT}-build") {
    switch(PLATFORM_NAME) {
        case ["macosx"]:
          sh("pwd")
          sh("env")

        break

        case ["windows-x86_64"]:
        bat (
            'dir' +
            '&&' +
            'set'
        )

        break

        default:

        break
    }

    if(CBUILD.toBoolean()) {
      switch(PLATFORM_NAME) {
          case ["macosx"]:
              parallel (
                  "Stream 0 ${LIBPROJECT}-Build-CPU-${PLATFORM_NAME}" : {
                      dir("stream0") {
                          sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                          dir("${LIBPROJECT}") {
                              // env.TRICK_NVCC = "YES"
                              env.LIBND4J_HOME = "${PWD}"

                              sh '''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              ./buildnativeoperations.sh
                              '''
                              stash includes: 'blasbuild/cpu/blas/', name: 'osx-cpu-blasbuild'
                              stash includes: 'blas/', name: 'osx-cpu-blas'
                          }
                      }
                  },
                  "Stream 1 ${LIBPROJECT}-Build-Cuda-${PLATFORM_NAME}" : {
                      dir("stream2") {
                          sh("cp -a ${WORKSPACE}/${LIBPROJECT} .\\")
                          dir("${LIBPROJECT}") {
                              // env.TRICK_NVCC = "YES"
                              env.LIBND4J_HOME = "${PWD}"
                              sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")

                              sh '''
                              ./buildnativeoperations.sh -c cuda -сс macosx
                              '''
                              stash includes: 'blasbuild/cuda/blas/', name: 'osx-cuda-blasbuild'
                              stash includes: 'blas/', name: 'osx-cuda-blas'

                          }
                      }
                  }
              )

              dir("libnd4j") {
                  unstash 'osx-cpu-blasbuild'
                  unstash 'osx-cpu-blas'
                  unstash 'osx-cuda-blasbuild'
                  unstash 'osx-cuda-blas'
              }
/*
      if(SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
      } else {
          echo "Skipping ${LIBPROJECT} checking with SonarQube"
      }
*/
      break

      case ["windows-x86_64"]:
          parallel (
              "Stream 1 Build CPU" : {
                  dir("stream1") {
                      bat("cp -a ${WORKSPACE}\\${LIBPROJECT} ${LIBPROJECT}")
                      bat '''
                      vcvars64.bat && bash buildnativeoperations.sh
                      '''
                      stash includes: 'blasbuild/cpu/blas/', name: 'win-cpu-blasbuild'
                      stash includes: 'include/', name: 'win-libnd4j-include'
                  }
              },
              "Stream 2 Build CUDA 7.5" : {
                  dir("stream2") {
                      bat("cp -a ${WORKSPACE}\\${LIBPROJECT} ${LIBPROJECT}")
                      bat '''
                      vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 7.5
                      '''
                      stash includes: 'blasbuild/cuda-7.5/blas/', name: 'win-cuda75-blasbuild'
                  }
              },
              "Stream 3 Build CUDA 8.0" : {
                  dir("stream3") {
                      bat("cp -a ${WORKSPACE}\\${LIBPROJECT} ${LIBPROJECT}")
                      bat '''
                      vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 8.0
                      '''
                      stash includes: 'blasbuild/cuda-8.0/blas/', name: 'win-cuda80-blasbuild'
                  }
              }
          )

      dir("${LIBPROJECT}") {
          unstash 'win-cpu-blasbuild'
          unstash 'win-cuda75-blasbuild'
          unstash 'win-cuda80-blasbuild'
          unstash 'win-libnd4j-include'
      }


      break

      default:
        error("Platform name - ${PLATFORM_NAME} is not defined or unsupported")

      break
    }
  } else {
    echo "Skipping ${LIBPROJECT} building"
  }
}

echo 'MARK: end of libnd4j.groovy'
