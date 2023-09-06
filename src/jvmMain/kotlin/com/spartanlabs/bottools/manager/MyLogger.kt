package com.spartanlabs.bottools.manager

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.spartanlabs.bottools.manager.viewModel as vm

class MyLogger(name: Class<*>, private val logger:Logger = LoggerFactory.getLogger(name)): Logger by logger{
    constructor(name:String):this(Class.forName(name), LoggerFactory.getLogger(name))
    override fun info(s:String){
        logger.info(s)
        vm.log += "$s\n"
    }
}