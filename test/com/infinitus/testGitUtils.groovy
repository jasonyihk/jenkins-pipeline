#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.pipeline.Env

class GitUtilsTest extends BasePipelineTest {
    def commit, script, host, deployConfig

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'
        super.setUp()        

        helper.registerAllowedMethod("sh", [Map.class], { c -> return commit})
        helper.registerAllowedMethod("error", [String.class], MockError)
        helper.registerAllowedMethod("file", [Map.class], MockFile)
		helper.registerAllowedMethod("withCredentials", [Map.class], MockWithCredentials)

        binding.setVariable('env', MockEnv)
        
        script = runScript("GitUtils.groovy") 

        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.log.env = env

        commit = '12345678'
        host = 'test.local'
        deployConfig = [
            dev: [
                    gitProjectKey: "PROJECTKEY", 
                    gitSlug: "REPO", 
                    gitUrl: "URL", 
                    gitBranch: "BRANCH", 
                    reqApproval: false 
            ],
            uat: [
                    gitProjectKey: "PROJECTKEY", 
                    gitSlug: "REPO", 
                    gitUrl: "URL", 
                    gitBranch: "BRANCH", 
                    reqApproval: true, 
            ]
        ]
    }

    @Test
    void testGetCommit() {
        assert commit == script.getCommit()
    }

    @Test
    void testGetCommitTag() {
        def tag = script.getCommitTags()
        assert tag instanceof String[]
        assert tag[0] == commit 
    }

    @Test
    void testAddKnownHost() {
        script.addKnownHost()
    }

}