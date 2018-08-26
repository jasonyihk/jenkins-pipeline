#!groovy

package com.pipeline

import groovy.transform.Field

@Field log = new Logger(className:"KubeDeploy")
@Field gitUtils = new GitUtils()
@Field bitBucket = new BitBucket()

def addKubeConfig(namespace) {
    try {
        def KUBE_CONFIG =  
            [ 
                dev : [
                    CONFIG: "${env.K8S_AWS_DEV_KUBECONFIG}",
                    CONTEXT: "${env.K8S_AWS_DEV_KUBECONFIG_CONTEXT}"
                ],
                uat : [
                    CONFIG: "${env.K8S_AWS_DEV_KUBECONFIG}",
                    CONTEXT: "${env.K8S_AWS_DEV_KUBECONFIG_CONTEXT}"
                ],
                stag : [
                    CONFIG: "${env.K8S_AWS_PROD_KUBECONFIG}",
                    CONTEXT: "${env.K8S_AWS_PROD_KUBECONFIG_CONTEXT}"
                ],
                prod : [
                    CONFIG: "${env.K8S_AWS_PROD_KUBECONFIG}",
                    CONTEXT: "${env.K8S_AWS_PROD_KUBECONFIG_CONTEXT}" 
                ],
                sit : [
                    CONFIG: "${env.K8S_PREM_DEV_KUBECONFIG}",
                    CONTEXT: "${env.K8S_PREM_DEV_KUBECONFIG_CONTEXT}",
                    SKIP_TLS: "${env.K8S_PREM_DEV_KUBE_SKIP_TLS}"
                ]                                               
            ]
        
        def kubeconfig = KUBE_CONFIG.find{ 
            it.key == namespace 
        }?.value

        assert kubeconfig != null
        assert kubeconfig.size()

        def kubefile = namespace + '.kubeconfig'
        withCredentials([file(credentialsId: "${kubeconfig.CONFIG}", variable: 'KUBECONFIG')]) {
    		sh """
				if [ ! -f ~/${kubefile} ]; then
					cp ${env.KUBECONFIG} ~/${kubefile} ;
					chmod 400 ~/${kubefile} ;
				fi
           	"""      
        }

        return kubeconfig.SKIP_TLS
    } catch (e) {
        error("failed to copy kube config file for namespace: ${namespace}")
    }
}

def deploy(namespace, commit, user) {
    try {       
        assert user != null
        assert commit != null
        assert namespace != null

        def flux = bitBucket.getFluxConfig(namespace)
        assert flux != null
        assert flux.gitUrl != null
        assert flux.gitBranch != null   
        assert flux.gitSlug != null    

		gitUtils.addKnownHost()
		gitUtils.addDefaultKey()

        def skip_tls = addKubeConfig(namespace) ?: false
        def kubefile = namespace + '.kubeconfig'   

        log.debug 'namespace: ' + namespace + ' flux: ' + flux
        def gitRepo = namespace + '_' + commit
        log.info 'start: clone [' + flux.gitUrl + '] to [' + gitRepo + ']'
        sh """
            rm -rf ${gitRepo}
            git clone ${flux.gitUrl} ${gitRepo}
            cd ${gitRepo}

            git checkout -f ${commit}

            files=(\$(git diff \$(git show | grep Merge: | cut -d":" -f2-) --pretty="" --name-only))
            if [[ \${#files[@]} -gt 0 ]]; then
                for file in \${files[@]} ; do
                    kubectl --kubeconfig=\$HOME/${kubefile} --insecure-skip-tls-verify=${skip_tls} -n ${namespace} apply -f \$file ;
                done
            fi    
        """
        log.info 'done: kubectl apply'

    } catch (e) {
        error("failed to deploy commit: [${commit}] to namespace [${namespace}], error:" + e.getMessage())   
    }
}

def release(namespace, user, tag) {
    try {       
        assert user != null
        assert tag != null
        assert namespace != null

        def flux = bitBucket.getFluxConfig(namespace)
        assert flux != null
        assert flux.gitUrl != null
        assert flux.gitBranch != null   
        assert flux.gitSlug != null    

		gitUtils.addKnownHost()
		gitUtils.addDefaultKey()

        def skip_tls = addKubeConfig(namespace) ?: false
        def kubefile = namespace + '.kubeconfig'   
        
        log.debug 'namespace: ' + namespace + ' flux: ' + flux

        Date today = new Date();
        def gitRepo = namespace + '_' + today.getTime()
        def releaseIndex = 'RELEASE'
        def releaseTag =  releaseIndex + '_' + tag
        log.info 'clone [' + flux.gitUrl + '] to [' + gitRepo + ']'
        sh """
            rm -rf ${gitRepo}
            git clone ${flux.gitUrl} ${gitRepo}
            cd ${gitRepo}

            git checkout -f ${flux.gitBranch}        

            lastReleaseTag=\$(git tag | grep ${releaseIndex} | xargs -I@ git log --format=format:"%ai @%n" -1 @ | sort -r |  head -n 1 | awk '{print \$4}')
            shouldTag=0
            if [[ -z "\$lastReleaseTag" ]]; then
                kubectl --kubeconfig=\$HOME/${kubefile} --insecure-skip-tls-verify=${skip_tls} -n ${namespace} apply -f .
                shouldTag=1
            else
                commit=\$(git rev-list -n 1 \$lastReleaseTag | cut -c1-8)
                files=(\$(git diff \$commit --pretty="" --name-only))
                if [[ \${#files[@]} -gt 0 ]]; then
                    for file in "\${files[@]}"
                    do
                        kubectl --kubeconfig=\$HOME/${kubefile} --insecure-skip-tls-verify=${skip_tls} -n ${namespace} apply -f \$file
                    done
                    shouldTag=1
                fi
            fi
            
            if [[ "\$shouldTag" -gt 0 ]]; then
                git config user.name '${user}'
                git config user.email '${user}@local'

                git tag -a ${releaseTag} -m '${user}'
                git push origin ${releaseTag}
            else
                echo 'no changes need to be applied.'
            fi    

        """
        log.info 'done kubectl apply'

    } catch (e) {
        error("failed to release master to namespace [${namespace}], error:" + e.getMessage())    
    }
}

return this