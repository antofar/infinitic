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

package io.infinitic.common.tasks.messages.workerMessages

import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskAttemptId
import io.infinitic.common.tasks.data.TaskAttemptIndex
import io.infinitic.common.tasks.data.TaskAttemptRetry
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.messages.interfaces.TaskAttemptMessage
import kotlinx.serialization.Serializable

@Serializable
sealed class WorkerMessage  {
    abstract val taskName: TaskName
}

@Serializable
data class RunTask(
    override val taskName: TaskName,
    override val taskId: TaskId,
    override val taskAttemptId: TaskAttemptId,
    override val taskAttemptRetry: TaskAttemptRetry,
    override val taskAttemptIndex: TaskAttemptIndex,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes,
    val methodInput: MethodInput,
    val taskOptions: TaskOptions,
    val taskMeta: TaskMeta
) : WorkerMessage(), TaskAttemptMessage