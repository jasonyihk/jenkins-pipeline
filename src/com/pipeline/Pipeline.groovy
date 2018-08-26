#!groovy
package com.pipeline

import hudson.model.JobProperty
import groovy.transform.Field

@Field gitUtils = new GitUtils()
@Field dockerUtils = new DockerUtils()
@Field controller = new Controller()
@Field log = new Logger(className:"Pipeline")

def executePipeline(pipelineDefinition = [:]) {
    this.pipelineDefinition = pipelineDefinition
	def _error_

	env.OM_PIPELINE_DEBUG = pipelineDefinition.debug ? pipelineDefinition.debug : false
    log.debug 'pipelineDefinition: ' + pipelineDefinition

	try {
		node(pipelineDefinition.agent) {
			stage("Init Properties") {
				setupProperties(pipelineDefinition)
			}
			
			stage('Cleanup') {
				deleteDir()
			}

			stage('Checkout') {
				gitUtils.addKnownHost()
				checkout scm
				gitUtils.addDefaultKey()
				gitUtils.addProjectKey()
			}

			def image = pipelineDefinition.docker.image
            def registry = pipelineDefinition.docker.registry ?: [:]
            def project = pipelineDefinition.docker.project ?: ''
			def commit = gitUtils.getCommit()
			assert image != null
			assert commit != null

			if(pipelineDefinition.stages && pipelineDefinition.stages.size()) {
				pipelineDefinition.stages.each { stg ->					
					log.debug 'stage: ' + stg

					def config = stg.value
					def label = config.label ? config.label : stg.key

					log.debug 'config: ' + config
					stage(label) {
						if(config.run == false) {
							println "stage [${label} is not enabled, [run: ${config.run}]. proceed to next stage "
						}
						else {
							def runScript = config.script
							assert runScript != null && runScript.length() > 0

							def runEnv = []
							if(config.tools != null && config.tools.size()) {
								config.tools.each { tl ->
									runEnv << "PATH+${tl.key}=${tool name: tl.value}/bin"
								}
							}
							
							log.debug 'runEnv: ' + runEnv
							withEnv(runEnv) {
								sh """
									${runScript}
								"""
							}
						}
					}
				}

                def shouldPushImage = false
                def shouldPushImageLatest = false
                if(pipelineDefinition.docker) {
                    if(pipelineDefinition.docker.shouldPush && pipelineDefinition.docker.shouldPush == true) {
                        shouldPushImage = true
                    }
                    
                    if(params.shouldDeployDEV && params.shouldDeployDEV == true) {
                        shouldPushImage = true
                    }   

                    if(params.shouldDeploySIT && params.shouldDeploySIT == true) {
                        shouldPushImage = true
                    }    

                    if(env.BRANCH_NAME.toLowerCase() == "master") {   
                        shouldPushImage = true                     
                        shouldPushImageLatest = true
                    }   
                }   

				def namespace = ''
				def tags = this.gitUtils.getCommitTags(commit)
				def topTag = null
                log.debug 'tags: ' + tags
                if(shouldPushImage) {
                    for (reg in registry) {
                        stage("Push Image to ${reg} - commit") {    
                            dockerUtils.push(reg, project, image, commit)     
                        }                            
                    }

                    if(tags && tags.length > 0) {
                        topTag = tags[0]
                        namespace = pipelineDefinition.docker.namespace ?: gitUtils.getNSfromTag(tag)

                        stage('Push Image - tag') {
                            dockerUtils.tag("${image}:${commit}", "${image}:${topTag}")
                            dockerUtils.push(namespace, project, image, topTag) 
                        }
                    }  

                    if(shouldPushImageLatest) {
                        for (reg in registry) {
                            stage("Push Image to ${reg} - latest") {    
                                dockerUtils.push(reg, project, image, commit)   
                                dockerUtils.tag("${image}:${commit}", "${image}:latest")
                                dockerUtils.push(namespace, project, image, 'latest') 
                            }   
                        }                            
                    }
                }

                def shouldDeploy = false
				def namespaces = controller.getDeployNS("${env.BRANCH_NAME}", topTag)
                if(params.shouldDeployDEV && params.shouldDeployDEV == true){
                    if(namespaces.contains('dev') == false){
                        namespaces << 'dev'  
                    }
                    shouldDeploy = true
                }  
                if(params.shouldDeploySIT && params.shouldDeploySIT== true){
                    if(namespaces.contains('sit') == false){
                        namespaces << 'sit'  
                    }
                    shouldDeploy = true
                }                   
                log.debug 'namespaces: ' + namespaces

                if(shouldDeploy && namespaces.size()) {    
                    def gitUrl = scm.getUserRemoteConfigs()[0].getUrl()

                    stage('Trigger PushKubeToFlux') {
                        def jobs = [:]
                        for (ns in namespaces) {
                            jobs["${ns}"] = transformIntoStep(gitUrl, commit, topTag, ns);
                        }

                        log.debug 'jobs: ' + jobs
                        parallel jobs
                    }
                }
                else {
					log.info 'no deployment needed, bye!'
				} 
                
				/*
				stage ('Starting Approval job') {
					build job: 'RunArtInTest', parameters: [[$class: 'StringParameterValue', name: 'systemname', value: systemname]]
				}
				*/

			} else {
				println "no stage found in the pipeline configuration. CONFIG: ${pipelineDefinition} "
			}
		}
	}
	catch (error) {
		log.error "Caught: ${error}"
		_error_ = error
		currentBuild.result = "FAILURE"
	} finally { 
		//mail to: team@example.com, subject: 'The Pipeline failed :('   
		if (_error_) {
			throw _error_
		}
	} 

}

def setupProperties(pipelineDefinition) {
	if(pipelineDefinition.jobProperties && pipelineDefinition.jobProperties.size()) {
		def propertiesArray = new ArrayList<JobProperty>();
		def propertiesToUse = controller.getProperties(pipelineDefinition.jobProperties)

		propertiesArray.add(parameters(propertiesToUse));
		
		//add default properties
		//propertiesArray.add(parameters([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '10', numToKeepStr: '5']]]))
    	
		properties(propertiesArray)

	}
}

def transformIntoStep(gitUrl, commit, tag, namespace) {
    return {
        build job: 'kube_to_flux',
            parameters: [
                string(name: "GIT_URL", value: gitUrl),
                string(name: "GIT_COMMIT", value: commit),
                string(name: "GIT_TAG", value: (tag == null) ? '' : tag),
                string(name: "DEPLOY_NAMESPACE", value: namespace)
            ],
            quietPeriod: 3
    }
}

return this