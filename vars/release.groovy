#!groovy
import com.pipeline.*
import groovy.transform.Field

@Field p = new KubeDeploy()

def call(namespace, user, tag) {
    p.release(namespace, user, tag)
}
