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

package io.infinitic.common.workflows.data.workflows

import io.infinitic.common.data.SerializedData
import io.infinitic.common.tasks.data.bases.Data
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WorkflowOutputSerializer::class)
data class WorkflowOutput(override val serializedData: SerializedData) : Data(serializedData) {
    companion object {
        fun from(data: Any?) = WorkflowOutput(SerializedData.from(data))
    }
}

object WorkflowOutputSerializer : KSerializer<WorkflowOutput> {
    override val descriptor: SerialDescriptor =  SerializedData.serializer().descriptor
    override fun serialize(encoder: Encoder, value: WorkflowOutput) {
        SerializedData.serializer().serialize(encoder,  value.serializedData)
    }
    override fun deserialize(decoder: Decoder) =
        WorkflowOutput(SerializedData.serializer().deserialize(decoder))
}
