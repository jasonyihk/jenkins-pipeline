#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.infiniuts.*

class ControllerTest extends BasePipelineTest {
    def script

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'
        super.setUp()        

        helper.registerAllowedMethod("error", [String.class], { msg -> throw new RuntimeException(msg)})
		helper.registerAllowedMethod("booleanParam", [Map.class], { 'boolean' })

        script = runScript("Controller.groovy") 
        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.log.metaClass.env = env
    }

	@Test
    void testGetProperties() {
        assert script.getProperties([shouldDeployDEV: true]).size() == 1
		assert script.getProperties([shouldDeployDEV: true])[0] == 'boolean'
		assert script.getProperties([shouldDeployDEV: false, shouldDeploySIT: true]).size() == 2
	}

    @Test
    void testGetDeployNS() {
        assert script.getDeployNS("feature", "123456").size() == 0
        assert script.getDeployNS("feature", "1.0.0-rc1").size() == 0
        assert script.getDeployNS("feature", "1.0.0").size() == 0
        assert script.getDeployNS("feature", "uat-1").size() == 0
        assert script.getDeployNS("feature", "hotfix-1.0.0").size() == 0

        def namespaces = script.getDeployNS("master", "123456")
        assert namespaces.size() ==  1
        assert namespaces.contains('dev')

        namespaces = script.getDeployNS("master", "uat-123")
        assert namespaces.size() ==  1
        assert namespaces.contains('uat')

        namespaces = script.getDeployNS("master", "1.0.0-rc1")
        assert namespaces.size() ==  1
        assert namespaces.contains('stag')

        namespaces = script.getDeployNS("master", "1.0.0")
        assert namespaces.size() ==  2
        assert namespaces.contains('stag')
        assert namespaces.contains('prod')

        namespaces = script.getDeployNS("master", "hotfix-1.0.0")
        assert namespaces.size() ==  3
        assert namespaces.contains('uat')
        assert namespaces.contains('stag')
        assert namespaces.contains('prod')
    }

}