#!groovy
import com.pipeline.*
import groovy.transform.Field

@Field p = new KubeDeploy()

def call(namespace, commit, user) {
    p.deploy(namespace, commit, user)
}
