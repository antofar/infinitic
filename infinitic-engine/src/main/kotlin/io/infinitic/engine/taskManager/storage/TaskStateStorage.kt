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

package io.infinitic.engine.taskManager.storage

import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.states.MonitoringGlobalState
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.common.tasks.states.TaskEngineState

/**
 * TaskStateStorage implementations are responsible for storing the different state objects used by the engine.
 *
 * No assumptions are made on whether the storage should be persistent or not, nor how the data should be
 * transformed before being stored. These details are left to the different implementations.
 */
interface TaskStateStorage {
    fun getMonitoringGlobalState(): MonitoringGlobalState?
    fun updateMonitoringGlobalState(newState: MonitoringGlobalState, oldState: MonitoringGlobalState?)
    fun deleteMonitoringGlobalState()

    fun getMonitoringPerNameState(taskName: TaskName): MonitoringPerNameState?
    fun updateMonitoringPerNameState(taskName: TaskName, newState: MonitoringPerNameState, oldState: MonitoringPerNameState?)
    fun deleteMonitoringPerNameState(taskName: TaskName)

    fun getTaskEngineState(taskId: TaskId): TaskEngineState?
    fun updateTaskEngineState(taskId: TaskId, newState: TaskEngineState, oldState: TaskEngineState?)
    fun deleteTaskEngineState(taskId: TaskId)
}
