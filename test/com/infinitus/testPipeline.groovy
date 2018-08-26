#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.pipeline.Env

class TestPipeline extends BasePipelineTest {
    def script
    def mockGitUtils = new GitUtils()
    
    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'

        super.setUp()        
        def scmBranch = "feature_test"
        helper.registerAllowedMethod("steps", [Closure.class], null)
        helper.registerAllowedMethod("post", [Closure.class], null)
        helper.registerAllowedMethod("success", [Closure.class], null)
        helper.registerAllowedMethod("failure", [Closure.class], null)
        helper.registerAllowedMethod("deleteDir",[], null)
        helper.registerAllowedMethod("skipStagesAfterUnstable", [], null)
        helper.registerAllowedMethod("skipDefaultCheckout", [Boolean.class], null)
        helper.registerAllowedMethod("findFiles", [Map.class], { file ->
            return file
        })
		helper.registerAllowedMethod("withEnv", [Object.class, Closure.class], null)
		helper.registerAllowedMethod("tool", [Map.class], null)
		helper.registerAllowedMethod("options", [Closure.class], null)
        helper.registerAllowedMethod("parameters", [Object.class], MockFunc)
        helper.registerAllowedMethod("build", [Map.class], {'build'})

        binding.setVariable('scm', [
                                        $class: 'GitSCM',
                                        branches: [[name: scmBranch]],
                                        getUserRemoteConfigs: { c -> return [[ getUrl: { return '' } ]] }
                                    ]                 
                            )   					

        mockGitUtils.env = MockEnv
        mockGitUtils.error = MockError
        mockGitUtils.file = MockFile
        mockGitUtils.withCredentials = MockWithCredentials
        mockGitUtils.sh = { c -> '' }

        script = runScript("Pipeline.groovy")
        script.gitUtils = mockGitUtils
        script.dockerUtils.DOCKER = true
        script.dockerUtils.docker = MockDocker
        script.dockerUtils.error =  MockError
		script.dockerUtils.env = MockEnv
        script.dockerUtils.sh = MockSh

        def env = new Env(OM_PIPELINE_DEBUG :true, BRANCH_NAME: 'master')
        script.env = env
        script.log.env = env
        script.gitUtils.log.env = env
        script.controller.log.env = env
        script.dockerUtils.log.env = env
        script.controller.booleanParam = MockFunc
        script.params = [shouldDeploy: true]
    }

    @Test
    void testTransformIntoStep() throws Exception {
        assert script.transformIntoStep('GIT_URL', 'GIT_COMMIT', null, 'dev')() == 'build'
		assert script.transformIntoStep('GIT_URL', 'GIT_COMMIT', 'GIT_TAG', 'dev')() == 'build'
    }

    @Test
    void testExecutePipeline() throws Exception {
		script.executePipeline([	
            agent: 'nodejs',
            debug: true,
            docker: [
                image: 'om.auth',
                buildPath: './build',
                shouldPush: true
            ],
            stages: [
                codeanalysis: [
                    label: 'Code Analysis',
                    run: true,
                    script: "make test-code-analysis"
                ],
                build: [
                    label: 'Build Package',
                    run: true,
                    script: "make build",
                    tools: [
                        NODE: 'N8'
                    ]
                ]		
            ],
            jobProperties: [
                shouldDeployDEV: true,
                shouldDeployDEV: false
            ]	
        ])

        printCallStack()
    }
}