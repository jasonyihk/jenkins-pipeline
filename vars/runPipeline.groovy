#!groovy
import com.pipeline.*
import groovy.transform.Field

@Field p = new Pipeline()

def call(pipelineDefinition) {
    p.executePipeline(pipelineDefinition)
}
