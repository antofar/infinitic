package io.infinitic.workflowManager.engine.avroConverter

import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage
import io.kotest.core.spec.TestConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import kotlin.reflect.KClass

class AvroConsistencyTests : StringSpec({
//    ForWorkflowEngineMessage::class.sealedSubclasses.forEach {
//        val msg = TestFactory.random(it)
//        include(checkAvroConversionToEnvelopeForWorkflowEngine(msg))
//    }
//
//    checkAllSubTypesFromEnvelope<ForWorkflowEngineMessage>(this, AvroEnvelopeForWorkflowEngine())
})

fun checkAvroConversionToEnvelopeForWorkflowEngine(msg: ForWorkflowEngineMessage) = stringSpec {
//    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForWorkflowEngine::class.simpleName}" {
//        shouldNotThrowAny {
//            AvroConverter.toWorkflowEngine(msg)
//        }
//    }
}

inline fun <reified T> checkAllSubTypesFromEnvelope(config: TestConfiguration, msg: GenericRecord) {
    msg.schema.getField("type").schema().enumSymbols.forEach {
        val schema = msg.schema.getField(it).schema()
        config.include(checkEnvelopeSchema(it, schema, msg::class))
        val name = schema.types[1].name
        val namespace = schema.types[1].namespace
        config.include(checkConversionFromAvro<T>(name, namespace))
    }
}

fun checkEnvelopeSchema(field: String, schema: Schema, klass: KClass<out GenericRecord>) = stringSpec {
    "Checking schema for field $field of ${klass.simpleName}" {
        // check that type is an union
        schema.isUnion shouldBe true
        // check that first type is null
        schema.types[0].isNullable shouldBe true
        // check size
        schema.types.size shouldBe 2
    }
}

inline fun <reified T> checkConversionFromAvro(name: String, namespace: String) = stringSpec {
//    "$name should be convertible from avro" {
//        // get class name
//        @Suppress("UNCHECKED_CAST")
//        val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
//        val message = AvroConverter.fromAvroMessage(TestFactory.random(klass))
//        (message is T) shouldBe true
//    }
}
