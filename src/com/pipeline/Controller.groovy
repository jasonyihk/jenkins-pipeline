#!groovy

package com.pipeline

import java.util.regex.Matcher
import java.util.regex.Pattern;
import groovy.transform.Field

@Field log = new Logger(className:"Controller")
def getProperties(pipelineProperties) {
	def jobProperties = []
	pipelineProperties.each { pr ->
        jobProperties << booleanParam(defaultValue: pr.value, description: "${pr.key}", name: "${pr.key}")
	}

	return jobProperties
}

def getDeployNS(branch, tag)  {
    def matcherUAT = tag ==~ /(?i)uat.*/
    def matcherRC = tag ==~ /(?i)(\d+)\.(\d+)\.(\d+)\-rc(\d+)/
    def matcherRelease = tag ==~ /(\d+)\.(\d+)\.(\d+)/
    def matcherHotfix = tag ==~ /(?i)hotfix\-(\d+)\.(\d+)\.(\d+)/
    def namespaces = []

    if(branch.equalsIgnoreCase('master') == true) {
        if(matcherUAT) {
            namespaces << 'uat'
        } else if (matcherRC) {
            namespaces << 'stag'
        } else if(matcherRelease) {
            namespaces << 'stag'
            namespaces << 'prod'
        } else if(matcherHotfix) {
            namespaces << 'uat'
            namespaces << 'stag'
            namespaces << 'prod'
        } else {
            namespaces << 'dev'
        }
    }

    log.debug 'namespaces: ' + namespaces
    return namespaces

}

return this