package net.codeocean.cheese.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory;

object Log {
     val logger: Logger? = LoggerFactory.getLogger(Log::class.java)

    fun logLevels(text:String) {
        logger?.error(text)
    }


}