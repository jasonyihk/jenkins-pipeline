#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.infiniuts.*

class BitBucketTest extends BasePipelineTest {
    def script

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'
        super.setUp()        

        script = runScript("BitBucket.groovy") 
        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.log.env = env
    }

    @Test
    void testGetFluxConfig() {
        ['dev', 'uat', 'stag', 'prod'].each {
            assert script.getFluxConfig(it).gitSlug != null
		    assert script.getFluxConfig(it).gitSlug == "k8s.flux.${it}"
        }
    }

    @Test
    void testSubmitPR() {
        script.submitPR('gitUrl', 'gitCommit', 'gitTag', 'dev')
    }

}