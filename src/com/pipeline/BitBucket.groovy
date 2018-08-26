#!groovy

package com.pipeline

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.transform.Field

@Field gitUtils = new GitUtils()
@Field log = new Logger(className:"BitBucket")

def getFluxConfig(namespace) {
    def FLUX = 
	[ 
		sit : [ 
            gitUrl : 'git@github.com:jasonyihk/k8s.flux.sit.git/',
            gitBranch : 'master',
            gitSlug : 'k8s.flux.sit'
		],
		dev : [ 
            gitUrl : 'git@github.com:jasonyihk/k8s.flux.dev.git/',
            gitBranch : 'master',
            gitSlug : 'k8s.flux.dev'
		],
		uat : [ 
            gitUrl : 'git@github.com:jasonyihk/k8s.flux.uat.git/',
            gitBranch : 'master',
            gitSlug : 'k8s.flux.uat'
		],        
		stag : [ 
            gitUrl : 'git@github.com:jasonyihk/k8s.flux.stag.git/',
            gitBranch : 'master',
            gitSlug : 'k8s.flux.stag'
		],
		prod : [ 
            gitUrl : 'git@github.com:jasonyihk/k8s.flux.prod.git/',
            gitBranch : 'master',
            gitSlug : 'k8s.flux.prod'
		]
	]
    
    return FLUX.find{ it.key == namespace }?.value
}

def submitPR(gitUrl, gitCommit, gitTag, namespace) {
    def flux = getFluxConfig(namespace)
    assert flux != null

    try {       
        assert gitUrl != null
        assert gitCommit != null
        assert namespace != null

        assert flux.gitSlug != null
        assert flux.gitUrl != null
        assert flux.gitBranch != null

		gitUtils.addKnownHost()
		gitUtils.addDefaultKey()

        log.debug 'namespace: ' + namespace + ' flux: ' + flux
		def gitRepoName = gitUrl.tokenize('/').last()
		def gitRepoRaw = gitRepoName.take(gitRepoName.lastIndexOf('.'))
        def gitRepo = gitRepoRaw + '_' + gitCommit + '_' + namespace

        log.info 'clone [' + gitUrl + '] to [' + gitRepo + ']'
        sh """
            rm -rf ${gitRepo}
            git clone ${gitUrl} ${gitRepo}
            cd ${gitRepo}

            git checkout -f ${gitCommit}
            IMAGE_TAG=${gitCommit}            
            if [ -n "${gitTag}" ]; then
                IMAGE_TAG=${gitTag}  
            fi
            NAMESPACE=${namespace} make kube-build
        """
        log.info 'done clone'

        def srcBranch = "${gitRepoRaw}/${gitCommit}"
        def commiter = gitUtils.getCommiter(gitRepo)
        def commiterEmail = gitUtils.getCommiterEmail(gitRepo)
        def commitMsg = gitUtils.getCommiterMsg(gitRepo)
		def user = "-c user.name='${commiter}' -c user.email='${commiterEmail}'"

        log.info 'clone [' + flux.gitUrl + '] to [flux-repo]'        
        sh """
			cd ${gitRepo}
            git clone ${flux.gitUrl} flux-repo
            cd flux-repo
            git checkout ${flux.gitBranch}
            git pull

			EXIST=\$(git ls-remote | grep -sw ${srcBranch} | wc -l)
			echo "is branch EXIST: \$EXIST"
			if [ "\$EXIST" -eq 0 ]; then 
				git checkout -b ${srcBranch} 
			else 
				git checkout ${srcBranch}
			fi

			cp ../build/${namespace}/*.* .
			DIRTY=\$(git status --porcelain | wc -l)
			echo "Is branch DIRTY: \$DIRTY"
			if [ "\$DIRTY" -gt 0 ]; then 
				git add -A
				git ${user} commit -a -m '${commitMsg}'
				git push -u origin ${srcBranch}
			fi 
        """
        def til = "[${gitRepoRaw}][${gitCommit}] requested by ${commiter}"
        def desc = "Jenkins on behalf of ${commiter} \nCommit Mesage: " + commitMsg
        def jsonData = [
            title: "${til}",
            description: "${desc}",
            close_source_branch: false,
            source: [
                branch: [
                    name: "${srcBranch}"
				]
			],
            destination: [
                branch: [
                    name: "${flux.gitBranch}"
				]
			]
		]

        def data = JsonOutput.toJson(jsonData)
        log.debug 'data: ' + data        
        callBitBucketAPI("/pullrequests", flux.gitSlug, 'POST', data)

    }
    catch (e) {
        error("failed to submit PR to remote: ${flux.gitUrl} error:" + e.getMessage())
    }    
}


def callBitBucketAPI(uri, gitSlug, method='get', data=null) {
	assert uri != null
	assert gitSlug != null

	def BITBUCKET_API= "curl -D- -u ${env.BITBUCKET_DEPLOY_USERID}:${env.BITBUCKET_DEPLOY_PASSWD} \
-H 'Content-Type: application/json' \
https://api.bitbucket.org/2.0/repositories/jasonyihk"

    log.debug 'BITBUCKET_API: ' + BITBUCKET_API
	def curl = "${BITBUCKET_API}/${gitSlug}"
	curl += "${uri}"
	if(method.equalsIgnoreCase('get') == false ) {
		assert data != null
		curl += " -X POST --data '${data}'"
	}

    log.debug 'curl: ' + curl

	return sh(script: "${curl}", returnStdout: true, returnStatus: true)
}

return this