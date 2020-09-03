package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.commands.Command
import io.infinitic.workflowManager.common.data.properties.PropertyValue
import io.infinitic.workflowManager.common.data.properties.PropertyName

data class WorkflowTaskOutput(
    val commands: List<Command> = listOf(),
    val updatedProperties: Map<PropertyName, PropertyValue> = mapOf()
)
