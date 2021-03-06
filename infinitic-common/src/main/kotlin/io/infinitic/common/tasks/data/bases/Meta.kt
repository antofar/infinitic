// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.common.tasks.data.bases

import com.fasterxml.jackson.annotation.JsonValue
import io.infinitic.common.data.SerializedData
import kotlin.reflect.full.primaryConstructor

abstract class Meta(open val data: Map<String, Any?> = mapOf()) {
    @get:JsonValue
    val json get() = when {
        this::serializedData.isInitialized -> serializedData
        else -> data.mapValues { SerializedData.from(it.value) }
    }

    lateinit var serializedData: Map<String, SerializedData>

    companion object {
        inline fun <reified T : Meta> fromSerialized(serialized: Map<String, SerializedData>) =
            T::class.primaryConstructor!!.call(serialized.mapValues { it.value.deserialize() }).apply {
                this.serializedData = serialized
            }
    }

    protected inline fun <reified T : Meta> withMeta(key: String, value: Any?): T {
        val newSerializedData = json.toMutableMap().plus(key to SerializedData.from(value)).toMap()
        return T::class.primaryConstructor!!.call(data.toMutableMap().plus(key to value).toMap()).apply {
            serializedData = newSerializedData
        }
    }
}
