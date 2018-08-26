#!groovy

package com.pipeline

import org.junit.Before
import org.junit.Test
import com.lesfurets.jenkins.unit.BasePipelineTest
import static Common.*
import com.infiniuts.*

class LoggerTest extends GroovyTestCase {
    def msg = 'test'
    def env = new Env(OM_PIPELINE_DEBUG:false)
    def logger = new Logger()

    @Before
    void setUp() throws Exception {
        logger.env = env
    }
    
	@Test
    void testDebug() {
        assert logger.debug(msg) == null

        logger.env.OM_PIPELINE_DEBUG = true
        assert logger.debug(msg).contains(msg)
        assert logger.debug(msg).contains('DEBUG')
	}

    @Test
    void testInfo() {
        assert logger.info(msg).contains(msg)
        assert logger.info(msg).contains('INFO')

        logger.env.OM_PIPELINE_DEBUG = true
        assert logger.info(msg).contains(msg)
        assert logger.info(msg).contains('INFO')
	}

    @Test
    void testError() {
        assert logger.error(msg).contains(msg)
        assert logger.error(msg).contains('ERROR')

        logger.env.OM_PIPELINE_DEBUG = true
        assert logger.error(msg).contains(msg)
        assert logger.error(msg).contains('ERROR')        
	}

    @Test
    void testWarn() {
        assert logger.warn(msg).contains(msg)
        assert logger.warn(msg).contains('WARN')

        logger.env.OM_PIPELINE_DEBUG = true
        assert logger.warn(msg).contains(msg)
        assert logger.warn(msg).contains('WARN')
	}
}