#!groovy

package com.pipeline

class Common {
    static MockError = { msg -> throw new RuntimeException(msg) }
    static MockFile = { options -> (options.getClass() == Map) ? [:] : '' }
    static MockSh = { c ->  (c.getClass() == Map) ? [:] : ''}
    static MockEnv = []
    static MockWithCredentials = {key, closure -> key}
    static MockStag = { label, closure -> label }
    static MockNode = { agent, closure -> agent }	
    static MockDocker = [
            build: { image, context -> image },
            withRegistry: {registry, credential, closure -> registry}
       ]
	static MockFunc = { c -> }   
}

class Env {
    Boolean OM_PIPELINE_DEBUG
    String BRANCH_NAME
    String BITBUCKET_PRIVATE_KEY_FILE_NAME
    String BITBUCKET_DEPLOY_USERID
    String BITBUCKET_DEPLOY_PASSWD
    String K8S_AWS_DEV_KUBECONFIG
    String K8S_AWS_DEV_KUBECONFIG_CONTEXT
    String K8S_AWS_UAT_KUBECONFIG
    String K8S_AWS_UAT_KUBECONFIG_CONTEXT
    String K8S_AWS_STAG_KUBECONFIG
    String K8S_AWS_STAG_KUBECONFIG_CONTEXT
    String K8S_AWS_PROD_KUBECONFIG
    String K8S_AWS_PROD_KUBECONFIG_CONTEXT
    String K8S_PREM_DEV_KUBECONFIG
    String K8S_PREM_DEV_KUBECONFIG_CONTEXT
    String K8S_PREM_DEV_KUBE_SKIP_TLS
}



