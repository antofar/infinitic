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

package io.infinitic.engine.taskManager.engines

import io.infinitic.common.fixtures.TestFactory
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskStatus
import io.infinitic.common.tasks.messages.monitoringGlobalMessages.TaskCreated
import io.infinitic.common.tasks.messages.monitoringPerNameMessages.TaskStatusUpdated
import io.infinitic.common.tasks.states.MonitoringPerNameState
import io.infinitic.engine.taskManager.storage.TaskStateStorage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll

class MonitoringPerNameTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<TaskStateStorage>()
            val dispatcher = mockk<Dispatcher>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to TaskStatus.RUNNING_OK,
                    "newStatus" to TaskStatus.RUNNING_ERROR
                )
            )
            val stateIn = TestFactory.random(MonitoringPerNameState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getMonitoringPerNameState(msg.taskName) } returns stateIn
            every { storage.updateMonitoringPerNameState(msg.taskName, capture(stateOutSlot), any()) } just runs

            val monitoringPerName = MonitoringPerName(storage, dispatcher)

            monitoringPerName.handle(msg)

            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getMonitoringPerNameState(msg.taskName)
                storage.updateMonitoringPerNameState(msg.taskName, stateOut, stateIn)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val storage = mockk<TaskStateStorage>()
            val dispatcher = mockk<Dispatcher>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to null,
                    "newStatus" to TaskStatus.RUNNING_OK
                )
            )
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getMonitoringPerNameState(msg.taskName) } returns null
            every { storage.updateMonitoringPerNameState(msg.taskName, capture(stateOutSlot), any()) } just runs
            coEvery { dispatcher.toMonitoringGlobal(any<TaskCreated>()) } just runs

            val monitoringPerName = MonitoringPerName(storage, dispatcher)

            // when
            monitoringPerName.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            coVerifyAll {
                storage.getMonitoringPerNameState(msg.taskName)
                storage.updateMonitoringPerNameState(msg.taskName, stateOut, null)
                dispatcher.toMonitoringGlobal(ofType<TaskCreated>())
            }
        }
    }
})
