#!groovy
import com.pipeline.*
import groovy.transform.Field

@Field p = new BitBucket()

def call(gitUrl, gitCommit, gitTag, namespace) {
    p.submitPR(gitUrl, gitCommit, gitTag, namespace)
}
