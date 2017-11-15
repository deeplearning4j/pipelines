package skymind.pipelines.projects

/**
 * Implements logic for initialization of project script class
 */
class ProjectFactory implements Serializable {
    static Project getProject(script, Map jobConfig) {
        /*
            Get project name to build from Jenkins job.
            JOB_NAME variables format contains full path to the job (including folders and sub-folders).
            JOB_BASE_NAME variables contains job name, in our case GitHub branch name.
            Logic for getting project name is valid only for GitHub Organization and Multibranch Pipeline job types.
         */
        String projectName = (script.env.JOB_NAME - script.env.JOB_BASE_NAME).tokenize('/').last().trim()

        switch (projectName) {
            case 'libnd4j':
                new Libnd4jProject(script, projectName, jobConfig).initPipeline()
                break
            case 'nd4j':
                jobConfig = jobConfig?.getAt('platforms') ?: [platforms: [
                        [backends  : ['cpu'],
                         compillers: [],
                         name      : 'android-x86'],

                        [backends  : ['cpu'],
                         compillers: [],
                         name      : 'android-arm'],

                        [backends  : ['cpu'],
                         compillers: [],
                         name      : 'linux-ppc64le'],

                        [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                         compillers: [],

                         name      : 'linux-x86_64'],

                        [backends  : ['cpu'],
                         compillers: [],
                         name      : 'macosx-x86_64'],

                        [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                         compillers: [],
                         name      : 'windows-x86_64']
                ]]
                new Nd4jProject(script, projectName, jobConfig).initPipeline()
                break
            case 'nd4s':
                new Nd4sProject(script, projectName, jobConfig).initPipeline()
                break
            case 'DataVec':
                new DataVecProject(script, projectName, jobConfig).initPipeline()
                break
            case 'deeplearning4j':
                new Deeplearning4jProject(script, projectName, jobConfig).initPipeline()
                break
            case 'Arbiter':
                new ArbiterProject(script, projectName, jobConfig).initPipeline()
                break
            case 'gym-java-client':
                new GymJavaClientProject(script, projectName, jobConfig).initPipeline()
                break
            case 'rl4j':
                new Rl4jProject(script, projectName, jobConfig).initPipeline()
                break
            case 'ScalNet':
                new ScalNetProject(script, projectName, jobConfig).initPipeline()
                break
            case 'lagom-skil-api':
                new LagomSkilApiProject(script, projectName, jobConfig).initPipeline()
                break
            default:
                throw new IllegalArgumentException('Project is not supported yet')
                break
        }
    }
}
