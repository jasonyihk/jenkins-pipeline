#!groovy

import groovy.transform.Field
import java.util.logging.Logger

@Field Logger logger = Logger.getLogger("SlackNotifier")
@Field settings = libraryResource 'config.json'

def getMessage(text){
   return "Job: ${env.JOB_NAME} with buildnumber ${env.BUILD_NUMBER} was ${buildResult}. message: ${text}"
}

def info(text) {
    slackSend (color: "good", channel: "${channel}", message: getMessage(text), teamDomain: '[company]', token: '[redacted]')
}

def success(text) {
    slackSend (color: "good", channel: "${channel}", message: getMessage(text), teamDomain: '[company]', token: '[redacted]')
}

def unstable(message) {
    slackSend (color: "warning", channel: "${channel}", message: getMessage(text), teamDomain: '[company]', token: '[redacted]')
}

def failure(message) {
    slackSend (color: "danger", channel: "${channel}", message: getMessage(text), teamDomain: '[company]', token: '[redacted]')
}

def call(String buildResult = 'SUCCESS', String channel, String text) {
    def color = "good"

    switch(buildResult) {
        case 'SUCCESS':
            color = 'good'
            break
        case 'FAILURE':
            color = 'danger'
            break
        case 'UNSTABLE':
            color = 'warning'
            break
        default: 
            color = 'danger'
    }
}