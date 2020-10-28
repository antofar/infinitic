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

package io.infinitic.common.workflows.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.methodRuns.MethodRunPosition
import io.infinitic.common.workflows.data.properties.PropertiesNameHash
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex

data class PastStep(
    val stepPosition: MethodRunPosition,
    val step: Step,
    val stepHash: StepHash,
    var stepStatus: StepStatus = StepStatusOngoing,
    var propertiesNameHashAtTermination: PropertiesNameHash? = null,
    var workflowTaskIndexAtTermination: WorkflowTaskIndex? = null
) {
    @JsonIgnore
    fun isTerminated() = stepStatus is StepStatusCompleted || stepStatus is StepStatusCanceled

    fun isTerminatedBy(pastCommand: PastCommand): Boolean {
        // returns false if already terminated
        if (isTerminated()) return false
        // apply update
        step.update(pastCommand.commandId, pastCommand.commandStatus)
        stepStatus = step.stepStatus()
        // returns true only if newly terminated
        return isTerminated()
    }

    fun isSimilarTo(newStep: NewStep) = newStep.stepHash == stepHash
}
