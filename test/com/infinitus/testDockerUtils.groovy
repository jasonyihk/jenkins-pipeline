#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.infiniuts.*

class DockerUtilsTest extends BasePipelineTest {
    def tag, image, script, context, namespace, registry

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'test/com/pipeline'
        scriptRoots += 'src/com/pipeline'
        super.setUp()

        binding.setVariable('DOCKER', 'true')
        binding.setVariable('docker', [
            build: {tag, context -> context},
            withRegistry: {registry, credential, closure -> registry}
        ])

        binding.setVariable('env', [
            AWS_ECR_REGISTRY_DEV: 'AWS_ECR_REGISTRY_DEV',
            AWS_ECR_REGISTRY_PROD: 'AWS_ECR_REGISTRY_PROD',
            AWS_ECR_REGION_DEV: 'AWS_ECR_REGION_DEV',
            AWS_ECR_REGION_PROD: 'AWS_ECR_REGION_PROD',
            AWS_ECR_CREDENTIAL_ID_DEV: 'AWS_ECR_CREDENTIAL_ID_DEV',
            AWS_ECR_CREDENTIAL_ID_PROD: 'AWS_ECR_CREDENTIAL_ID_PROD'
        ])
 
        script = runScript("DockerUtils.groovy") 
        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.log.metaClass.env = env

        image = 'om.test'
        tag = 'latest'
        context = '-f Dockerfile .'
        namespace = 'dev'
        registry = 'https://AWS_ECR_REGISTRY_DEV'

        helper.registerAllowedMethod("error", [String.class], { msg -> throw new RuntimeException(msg)})
    }

    @Test
    void testBuild() {
        assert script.build(image, tag) == '--network=host .'
        assert script.build(image, tag, context).contains(context) == true
    }

    @Test
    void testPush() {
        assert script.push(namespace, '', image, tag) == registry
    }
}