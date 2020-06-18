package com.zenaton.workflowManager.pulsar.messages

import com.zenaton.workflowManager.pulsar.topics.workflows.messages.WorkflowMessageContainer
import com.zenaton.workflowManager.messages.TaskCompleted
import com.zenaton.workflowManager.messages.WorkflowDispatched
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class WorkflowMessageContainerTests : StringSpec({

    "WorkflowMessageContainer must throw containing more than one message" {
        val wd = mockk<WorkflowDispatched>()
        val tc = mockk<TaskCompleted>()
        // given
        val container = WorkflowMessageContainer(wd)
        container.taskCompleted = tc
        // when, then
        shouldThrowAny {
            container.msg()
        }
    }

    "WorkflowMessageContainer must provide msg" {
        val wd = mockk<WorkflowDispatched>()
        // given
        val container = WorkflowMessageContainer(wd)
        // when, then
        shouldNotThrowAny {
            val msg = container.msg()
            msg shouldBe wd
        }
    }
})