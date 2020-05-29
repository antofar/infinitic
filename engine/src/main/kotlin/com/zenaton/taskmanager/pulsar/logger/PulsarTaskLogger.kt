package com.zenaton.taskmanager.pulsar.logger

import com.zenaton.commons.utils.json.Json
import com.zenaton.taskmanager.logger.TaskLogger
import com.zenaton.taskmanager.pulsar.Topic
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.functions.api.Context
import org.slf4j.Logger

class PulsarTaskLogger(val context: Context) : TaskLogger {
    // Json injection
    private var json = Json
    // Logger injection
    private var logger: Logger = context.logger

    override fun debug(txt: String, obj1: Any?, obj2: Any?): String {
        val text = getText(txt, obj1, obj2)
        if (logger.isDebugEnabled) { logger.debug(text) }
        dispatch(context, text, "DEBUG")
        return text
    }

    override fun error(txt: String, obj1: Any?, obj2: Any?): String {
        val text = getText(txt, obj1, obj2)
        if (logger.isErrorEnabled) { logger.error(text) }
        dispatch(context, text, "ERROR")
        return text
    }

    override fun info(txt: String, obj1: Any?, obj2: Any?): String {
        val text = getText(txt, obj1, obj2)
        if (logger.isInfoEnabled) { logger.info(text) }
        dispatch(context, text, "INFO")
        return text
    }

    override fun warn(txt: String, obj1: Any?, obj2: Any?): String {
        val text = getText(txt, obj1, obj2)
        if (logger.isWarnEnabled) { logger.warn(text) }
        dispatch(context, text, "WARN")
        return text
    }

    override fun trace(txt: String, obj1: Any?, obj2: Any?): String {
        val text = getText(txt, obj1, obj2)
        if (logger.isTraceEnabled) { logger.trace(text) }
        dispatch(context, text, "TRACE")
        return text
    }

    private fun getText(txt: String, obj1: Any?, obj2: Any?): String {
        val var1 = if (obj1 == null) "" else "\n${json.stringify(obj1, pretty = true)}\n"
        val var2 = if (obj2 == null) "" else "\n${json.stringify(obj2, pretty = true)}\n"

        return String.format(txt, var1, var2)
    }

    private fun dispatch(context: Context, msg: String, level: String): MessageId {
        return context
            .newOutputMessage(Topic.LOGS.get(), Schema.STRING)
            .value("$level - $msg")
            .send()
    }
}
