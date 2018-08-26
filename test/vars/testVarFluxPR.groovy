#!groovy

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.pipeline.Common.*
import com.pipeline.*

class TestVarFluxPR extends BasePipelineTest {
    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'vars'

        super.setUp()   
    }

    @Test
    void testVarFluxPR() throws Exception {
        Script script = helper.loadScript("submitFluxPR.groovy") 

        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.p.env = env
        script.p.log.metaClass.env = env

        script.p.error = { String msg -> throw new RuntimeException(msg)}
        script.p.sh = MockSh
        script.p.gitUtils.sh = MockSh
		script.p.gitUtils.env = env
		script.p.gitUtils.error = { String msg -> throw new RuntimeException(msg)}
		script.p.gitUtils.file = MockFile
		script.p.gitUtils.withCredentials = MockWithCredentials

        script('gitUrl', 'gitCommit', 'gitTag', 'dev')

        printCallStack()
    }


}