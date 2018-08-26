#!groovy

package com.pipeline

import groovy.transform.Field

@Field log = new Logger(className:"DockerUtils")
def build(image, tag, context = '.') {
    docker.build("${image}:${tag}", "--network=host ${context}")
}

def tag(imageTag, newImageTag) {
    sh """
        docker tag ${imageTag} ${newImageTag} 
    """
}

def push(reg, project, image, imageTag) {
    assert reg != null
    assert image != null
    assert imageTag != null

    log.debug 'registry:[' + reg + '] image:[' + image + '] tag:[' + imageTag + ']'
    def repo = getRegistry(reg)
    if(repo == null){
        error("no registry config found for registry: ${reg}, bail out")
    }

    log.debug 'registry: ' + repo
    def credential = repo.CREDENTIAL_ID
    if(repo.ECR) {
        credential = "ecr:${repo.REGION}:" + credential
    }
    log.debug 'credential: ' + credential
    
    docker.withRegistry("https://${repo.REGISTRY}", "${credential}") {
        def imageName = "${image}:${imageTag}"
        if(repo.PROJECT) {
            imageName = project + "/" + imageName
            tag("${image}:${imageTag}", imageName)
        }

        img = docker.image(imageName)  
        img.push(imageTag)  
    }
}

def transfer(reg, namespace, project, image, commit, tag) {
    assert reg != null
    assert image != null
    assert tag != null

    def regSource = getRegistry(reg)
    if(regSource == null){
        error("no registry config found for: ${reg}, bail out")
    }
    log.debug 'Source Registry: ' + regSource

    def regTarget = getRegistry(namespace)
    if(regTarget == null){
        error("no registry config found for: ${namespace}, bail out")
    }
    log.debug 'Target Registry: ' + regTarget

    def credSource = regSource.CREDENTIAL_ID
    if(regSource.ECR) {
        credSource = "ecr:${regSource.REGION}:" + credSource
    }
    log.debug 'Source Credential: ' + credSource

    def credTarget = regTarget.CREDENTIAL_ID
    if(regTarget.ECR) {
        credTarget = "ecr:${regTarget.REGION}:" + credTarget
    }
    log.debug 'Target Credential: ' + credTarget    

    def imageTagSource = "${image}:${tag}"
    if(regSource.PROJECT) {
        imageTagSource = project + "/" + imageTagSource
    }    
    docker.withRegistry("https://${regSource.REGISTRY}", "${credSource}") {
        sh """
            docker pull ${regSource.REGISTRY}/${imageTagSource}
        """    
    }

    def imageTagTarget = "${image}:${tag}"
    if(regTarget.PROJECT) {
        imageTagTarget = project + "/" + imageTagTarget
    }  
    docker.withRegistry("https://${regTarget.REGISTRY}", "${credTarget}") {
        sh """
            docker tag ${regSource.REGISTRY}/${imageTagSource} ${regTarget.REGISTRY}/${imageTagTarget}
            docker push ${regTarget.REGISTRY}/${imageTagTarget}
        """    
    }

}

def getRegistry(namespace) {
	def REGISTRY =  
	[ 
		aws : [ 
				REGISTRY : "${env.AWS_ECR_REGISTRY_DEV}",
				REGION : "${env.AWS_ECR_REGION_DEV}",
				CREDENTIAL_ID : "${env.AWS_ECR_CREDENTIAL_ID_DEV}",
                ECR: true,
                PROJECT: false
		],
        harbor : [
          		REGISTRY : "${env.HARBOR_REGISTRY}",
				REGION : "",
				CREDENTIAL_ID : "${env.HARBOR_CREDENTIAL_ID}" ,
                ECR: false,
                PROJECT: true
        ], 
        sit : [
          		REGISTRY : "${env.HARBOR_REGISTRY}",
				REGION : "",
				CREDENTIAL_ID : "${env.HARBOR_CREDENTIAL_ID}" ,
                ECR: false,
                PROJECT: true
        ],                
		dev : [ 
				REGISTRY : "${env.AWS_ECR_REGISTRY_DEV}",
				REGION : "${env.AWS_ECR_REGION_DEV}",
				CREDENTIAL_ID : "${env.AWS_ECR_CREDENTIAL_ID_DEV}",
                ECR: true,
                PROJECT: false
        ],   
        uat : [ 
				REGISTRY : "${env.AWS_ECR_REGISTRY_UAT}",
				REGION : "${env.AWS_ECR_REGION_UAT}",
				CREDENTIAL_ID : "${env.AWS_ECR_CREDENTIAL_ID_UAT}",
                ECR: true,
                PROJECT: false
        ],        
		stag : [ 
				REGISTRY : "${env.AWS_ECR_REGISTRY_STAG}",
				REGION : "${env.AWS_ECR_REGION_STAG}",
				CREDENTIAL_ID : "${env.AWS_ECR_CREDENTIAL_ID_STAG}",
                ECR: true,
                PROJECT: false
		],
		prod : [ 
				REGISTRY : "${env.AWS_ECR_REGISTRY_PROD}",
				REGION : "${env.AWS_ECR_REGION_PROD}",
				CREDENTIAL_ID : "${env.AWS_ECR_CREDENTIAL_ID_PROD}",
                ECR: true,
                PROJECT: false
		]
	]
	
	return REGISTRY.find{ it.key == namespace }?.value
}

return this