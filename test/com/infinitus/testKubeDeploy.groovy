#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.infiniuts.*

class KubeDeployTest extends BasePipelineTest {
    def script

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'
        super.setUp()        

        helper.registerAllowedMethod("sh", [Map.class], { c -> return commit})
        script = runScript("KubeDeploy.groovy") 
    }

    @Test
    void testKubeDeploy() {
        script.deploy('dev', '1234567', 'user') 
    }

    @Test
    void testKubeRelease() {
        script.release('dev', 'user', 'tag')
    }

}