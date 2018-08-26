#!groovy

package com.pipeline

import java.text.SimpleDateFormat 
import groovy.transform.Field

@Field className = 'default'
def debug(args) {
    if(env.OM_PIPELINE_DEBUG) {
        printMsg('debug', args)
    }
}

def error(args) {
    printMsg('error', args)
}

def info(args) {
    printMsg('info', args)
}

def warn(args) {
    printMsg('warn', args)
}

def methodMissing(name, argrs) {
    printMsg(name, argrs)
}
/**
 * Catch all defined logging levels, throw  MissingMethodException otherwise
 * @param label
 * @param args
 * @return
 */
boolean isCollectionOrArray(object) {    
    [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
}

def printMsg(label, args) {
    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS")
    String date = formatter.format(new Date())
    def String message = "[${label.toUpperCase()}][${className}] ${date} "
    if(isCollectionOrArray(args)) {
        args.each { arg ->
            message += " ${arg}"
        }
    }
    else {
        message += args
    }
    message += "\n"
    
    println message
    return message
}

return this

