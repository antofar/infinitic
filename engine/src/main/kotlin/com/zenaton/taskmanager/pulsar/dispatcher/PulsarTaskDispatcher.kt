package com.zenaton.taskmanager.pulsar.dispatcher

import com.zenaton.taskmanager.dispatcher.TaskDispatcher
import com.zenaton.taskmanager.messages.engine.AvroTaskEngineMessage
import com.zenaton.taskmanager.messages.engine.TaskEngineMessage
import com.zenaton.taskmanager.messages.metrics.AvroTaskMetricMessage
import com.zenaton.taskmanager.messages.metrics.TaskMetricMessage
import com.zenaton.taskmanager.messages.workers.AvroTaskWorkerMessage
import com.zenaton.taskmanager.messages.workers.TaskWorkerMessage
import com.zenaton.taskmanager.pulsar.Topic
import com.zenaton.taskmanager.pulsar.avro.TaskAvroConverter
import java.util.concurrent.TimeUnit
import org.apache.pulsar.client.impl.schema.AvroSchema
import org.apache.pulsar.functions.api.Context

/**
 * This object provides a 'dispatch' method to send a task message into the tasks topic
 */
class PulsarTaskDispatcher(val context: Context) : TaskDispatcher {

    /**
     *  Workers message
     */
    override fun dispatch(msg: TaskWorkerMessage) {
        context
            .newOutputMessage(Topic.WORKERS.get(msg.taskName.name), AvroSchema.of(AvroTaskWorkerMessage::class.java))
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Metrics message
     */
    override fun dispatch(msg: TaskMetricMessage) {
        context
            .newOutputMessage(Topic.METRICS.get(), AvroSchema.of(AvroTaskMetricMessage::class.java))
            .key(msg.getStateId())
            .value(TaskAvroConverter.toAvro(msg))
            .send()
    }

    /**
     *  Engine messages
     */
    override fun dispatch(msg: TaskEngineMessage, after: Float) {

        val msgBuilder = context
            .newOutputMessage(Topic.ENGINE.get(), AvroSchema.of(AvroTaskEngineMessage::class.java))
            .key(msg.getStateId())
            .value(TaskAvroConverter.toAvro(msg))

        if (after > 0F) {
            msgBuilder.deliverAfter((after * 1000).toLong(), TimeUnit.MILLISECONDS)
        }

        msgBuilder.send()
    }
}