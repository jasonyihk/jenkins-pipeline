#!groovy

package com.pipeline

import groovy.transform.Field

@Field log = new Logger(className:"GitUtils")
def addKnownHost() {
	sh """
	    if [ ! -f ~/.ssh/config ]; then		
			mkdir -p ~/.ssh ;
			echo \"Host bitbucket.org\" >> ~/.ssh/config ;
			echo \"    StrictHostKeyChecking no\" >> ~/.ssh/config ;
			echo \"\" >> ~/.ssh/config
		fi	
	"""
}

def getCommit() {
    sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true)?.trim()
         .replaceAll("[^0-9a-zA-Z_\\.]", "-").toLowerCase()
}

def getCommiter(path) {
    def cmd = ''
    if(path) {
        cmd = "cd ${path} ; \n"
    }
    cmd = cmd + 'git --no-pager show -s --format=\'%an\' ; '
    sh(script: cmd, returnStdout: true)?.trim()
}

def getCommiterEmail(path) {
    def cmd = ''
    if(path) {
        cmd = "cd ${path} ; \n"
    }
    cmd = cmd + 'git --no-pager show -s --format=\'%ae\' ; '
    sh(script: cmd, returnStdout: true)?.trim()
}

def getCommiterMsg(path) {
    def cmd = ''
    if(path) {
        cmd = "cd ${path} ; \n"
    }
    cmd = cmd + 'git --no-pager show -s --format=\'%B\' ; '
    sh(script: cmd, returnStdout: true)?.trim()
}

def getCommitTags(commit) {
    sh(script: "git tag --contains ${commit} --sort=-v:refname", returnStdout: true)?.split()
}

def addDefaultKey() {
    try {
        withCredentials([file(credentialsId: "${env.BITBUCKET_PRIVATE_KEY_FILE_NAME}", variable: 'KEYFILE')]) {
    		sh """
				if [ ! -f ~/.ssh/id_rsa ]; then
					mkdir -p ~/.ssh ;
					cp ${env.KEYFILE} ~/.ssh/id_rsa ;
					chmod 400 ~/.ssh/id_rsa ;
				fi
           	"""    
        }
    } catch (e) {
        error("failed to copy key file: ${env.BITBUCKET_PRIVATE_KEY_FILE_NAME}, ${e.getMessage()}")
    }
}

def addProjectKey() {
    try {
        withCredentials([file(credentialsId: "${env.BITBUCKET_PRIVATE_KEY_FILE_NAME}", variable: 'KEYFILE')]) {
    		sh """
				if [ ! -f keys/id_rsa ]; then
					mkdir -p keys ;
					cp ${env.KEYFILE} keys/id_rsa ;
					chmod 400 keys/id_rsa ;
				fi
           	"""    
        }
    } catch (e) {
        error("failed to copy key file: ${env.BITBUCKET_PRIVATE_KEY_FILE_NAME}, ${e.getMessage()}")
    }
}

def cleanUp() {
    try {
		keyPath.each { ph ->
			sh """
				mkdir -p ${ph} 
				cp ${env.KEYFILE} ${ph}/id_rsa
			"""    
		}
    } catch (e) {
        error("failed to clean up, ${e.getMessage()}")
    }
}

return this