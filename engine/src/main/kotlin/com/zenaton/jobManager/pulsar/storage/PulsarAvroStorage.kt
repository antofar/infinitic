package com.zenaton.jobManager.pulsar.storage

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.jobManager.data.JobStatus
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.states.AvroEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import org.apache.pulsar.functions.api.Context

class PulsarAvroStorage(val context: Context) : AvroStorage {
    // serializer injection
    var avroSerDe = AvroSerDe

    override fun getEngineState(jobId: String): AvroEngineState? {
        return context.getState(getEngineStateKey(jobId))?.let { avroSerDe.deserialize<AvroEngineState>(it) }
    }

    override fun updateEngineState(jobId: String, newState: AvroEngineState, oldState: AvroEngineState?) {
        context.putState(getEngineStateKey(jobId), avroSerDe.serialize(newState))
    }

    override fun deleteEngineState(jobId: String) {
        context.deleteState(getEngineStateKey(jobId))
    }

    override fun getMonitoringPerNameState(jobName: String): AvroMonitoringPerNameState? =
        context.getState(getMonitoringPerNameStateKey(jobName))?.let { avroSerDe.deserialize<AvroMonitoringPerNameState>(it) }

    override fun updateMonitoringPerNameState(jobName: String, newState: AvroMonitoringPerNameState, oldState: AvroMonitoringPerNameState?) {
        val counterOkKey = getMonitoringPerNameCounterKey(jobName, JobStatus.RUNNING_OK)
        val counterWarningKey = getMonitoringPerNameCounterKey(jobName, JobStatus.RUNNING_WARNING)
        val counterErrorKey = getMonitoringPerNameCounterKey(jobName, JobStatus.RUNNING_ERROR)
        val counterCompletedKey = getMonitoringPerNameCounterKey(jobName, JobStatus.TERMINATED_COMPLETED)
        val counterCanceledKey = getMonitoringPerNameCounterKey(jobName, JobStatus.TERMINATED_CANCELED)

        // use counters to save state, to avoid race conditions
        val incrOk = newState.runningOkCount - (oldState?.runningOkCount ?: 0L)
        val incrWarning = newState.runningWarningCount - (oldState?.runningWarningCount ?: 0L)
        val incrError = newState.runningErrorCount - (oldState?.runningErrorCount ?: 0L)
        val incrCompleted = newState.terminatedCompletedCount - (oldState?.terminatedCompletedCount ?: 0L)
        val incrCanceled = newState.terminatedCanceledCount - (oldState?.terminatedCanceledCount ?: 0L)

        if (incrOk != 0L) context.incrCounter(counterOkKey, incrOk)
        if (incrWarning != 0L) context.incrCounter(counterWarningKey, incrWarning)
        if (incrError != 0L) context.incrCounter(counterErrorKey, incrError)
        if (incrCompleted != 0L) context.incrCounter(counterCompletedKey, incrCompleted)
        if (incrCanceled != 0L) context.incrCounter(counterCanceledKey, incrCanceled)

        // save state retrieved from counters
        val state = AvroMonitoringPerNameState.newBuilder().apply {
            setJobName(jobName)
            runningOkCount = context.getCounter(counterOkKey)
            runningWarningCount = context.getCounter(counterWarningKey)
            runningErrorCount = context.getCounter(counterErrorKey)
            terminatedCompletedCount = context.getCounter(counterCompletedKey)
            terminatedCanceledCount = context.getCounter(counterCanceledKey)
        }.build()

        context.putState(getMonitoringPerNameStateKey(jobName), avroSerDe.serialize(state))
    }

    override fun deleteMonitoringPerNameState(jobName: String) {
        context.deleteState(getMonitoringPerNameStateKey(jobName))
    }

    override fun getMonitoringGlobalState(): AvroMonitoringGlobalState? {
        return context.getState(getMonitoringGlobalStateKey())?.let { avroSerDe.deserialize<AvroMonitoringGlobalState>(it) }
    }

    override fun updateMonitoringGlobalState(newState: AvroMonitoringGlobalState, oldState: AvroMonitoringGlobalState?) {
        context.putState(getMonitoringGlobalStateKey(), avroSerDe.serialize(newState))
    }

    override fun deleteMonitoringGlobalState() {
        context.deleteState(getMonitoringGlobalStateKey())
    }

    fun getEngineStateKey(jobId: String) = "engine.state.$jobId"
    fun getMonitoringGlobalStateKey() = "monitoringGlobal.state"
    fun getMonitoringPerNameStateKey(jobName: String) = "monitoringPerName.state.$jobName"
    fun getMonitoringPerNameCounterKey(jobName: String, jobStatus: JobStatus) = "monitoringPerName.counter.${jobStatus.toString().toLowerCase()}.${jobName.toLowerCase()}"
}