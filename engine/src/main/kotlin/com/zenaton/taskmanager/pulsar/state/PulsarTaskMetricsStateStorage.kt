package com.zenaton.taskmanager.pulsar.state

import com.zenaton.commons.utils.avro.AvroSerDe
import com.zenaton.taskmanager.data.TaskName
import com.zenaton.taskmanager.data.TaskStatus
import com.zenaton.taskmanager.metrics.state.TaskMetricsState
import com.zenaton.taskmanager.metrics.state.TaskMetricsStateStorage
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import org.apache.pulsar.functions.api.Context

class PulsarTaskMetricsStateStorage(val context: Context) : TaskMetricsStateStorage {
    var avroSerDe = AvroSerDe
    var avroConverter = TaskAvroConverter

    override fun updateTaskStatusCountersByName(taskName: TaskName, oldStatus: TaskStatus?, newStatus: TaskStatus) {
        oldStatus?.let {
            incrCounter(getCounterKey(taskName, it), -1)
        }

        incrCounter(getCounterKey(taskName, newStatus), 1)

        val state = TaskMetricsState(taskName)
        state.runningOkCount = getCounter(getCounterKey(taskName, TaskStatus.RUNNING_OK))
        state.runningWarningCount = getCounter(getCounterKey(taskName, TaskStatus.RUNNING_WARNING))
        state.runningErrorCount = getCounter(getCounterKey(taskName, TaskStatus.RUNNING_ERROR))
        state.terminatedCompletedCount = getCounter(getCounterKey(taskName, TaskStatus.TERMINATED_COMPLETED))
        state.terminatedCanceledCount = getCounter(getCounterKey(taskName, TaskStatus.TERMINATED_CANCELED))

        putState(getStateKey(taskName), state)
    }

    private fun putState(key: String, state: TaskMetricsState) = context.putState(key, avroSerDe.serialize(avroConverter.toAvro(state)))

    private fun incrCounter(key: String, amount: Long) = context.incrCounter(key, amount)

    private fun getCounter(key: String) = context.getCounter(key)

    private fun getCounterKey(taskName: TaskName, taskStatus: TaskStatus) = "metrics.rt.counter.task.${taskName.name.toLowerCase()}.${taskStatus.toString().toLowerCase()}"

    private fun getStateKey(taskName: TaskName) = "metrics.task.${taskName.name.toLowerCase()}.counters"
}