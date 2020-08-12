package com.zenaton.jobManager.worker

import com.zenaton.jobManager.common.data.JobOptions
import java.lang.reflect.Method

data class JobCommand(
    val job: Any,
    val method: Method,
    val parameters: Array<Any?>,
    val options: JobOptions
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JobCommand

        if (job != other.job) return false
        if (method != other.method) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = job.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + options.hashCode()
        return result
    }
}
