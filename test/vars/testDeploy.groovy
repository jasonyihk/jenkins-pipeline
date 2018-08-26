#!groovy

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.pipeline.Common.*
import com.pipeline.*

class TestDeploy extends BasePipelineTest {
    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'vars'

        super.setUp()   
    }

    @Test
    void testDeploy() throws Exception {
        Script script = helper.loadScript("deploy.groovy") 

        def env = new Env(OM_PIPELINE_DEBUG :true,
            K8S_AWS_DEV_KUBECONFIG: '',
            K8S_AWS_DEV_KUBECONFIG_CONTEXT: '',
            K8S_AWS_UAT_KUBECONFIG: '',
            K8S_AWS_UAT_KUBECONFIG_CONTEXT: '',
            K8S_AWS_STAG_KUBECONFIG: '',
            K8S_AWS_STAG_KUBECONFIG_CONTEXT: '',
            K8S_AWS_PROD_KUBECONFIG: '',
            K8S_AWS_PROD_KUBECONFIG_CONTEXT: '',
            K8S_PREM_DEV_KUBECONFIG: '',
            K8S_PREM_DEV_KUBECONFIG_CONTEXT: ''
        )
        script.p.env = env
        script.p.log.metaClass.env = env

        script.p.error = { String msg -> throw new RuntimeException(msg)}
        script.p.sh = MockSh
        script.p.withCredentials = MockWithCredentials
        script.p.file = MockFile
        script.p.gitUtils.sh = MockSh
		script.p.gitUtils.env = env
		script.p.gitUtils.error = { String msg -> throw new RuntimeException(msg)}
		script.p.gitUtils.file = MockFile
		script.p.gitUtils.withCredentials = MockWithCredentials

        script('dev', '1234567', 'user')

        printCallStack()
    }


}