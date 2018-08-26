#!groovy

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static com.pipeline.Common.*
import com.pipeline.*

class TestVarPipeline extends BasePipelineTest {
    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'vars'

        super.setUp()     
    }

    @Test
    void testVarPipeline() throws Exception {
        Script script = helper.loadScript("runPipeline.groovy")  
		script.p.disableConcurrentBuilds = MockFunc
		script.p.skipStagesAfterUnstable = MockFunc
		script.p.skipDefaultCheckout =  { c -> null }
		script.p.logRotator =  MockFunc
		script.p.buildDiscarder = MockFunc
		script.p.node = MockNode
		script.p.options =  { c -> null }
		script.p.currentBuild = []

        def env = new Env(OM_PIPELINE_DEBUG :true)
        script.p.env = env
        script.p.log.metaClass.env = env

        script([	
		   	agent: 'nodejs',
			shouldDeploy: true,
			docker: [
				image: 'pipe-test',
				buildPath: './build'
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
					script: "make build"	
				]		
			],
			properties: [

			]
        ])

        printCallStack()
    }


}