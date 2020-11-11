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

package io.infinitic.common.tasks.exceptions

import io.infinitic.common.json.Json
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.Constants
import io.infinitic.common.tasks.data.TaskOptions
import kotlinx.serialization.Serializable

/*
Base class for any exception due to user
(Must be an unchecked exception, to avoid UndeclaredThrowableException when thrown from a proxy)
 */
@Serializable
sealed class UserException : RuntimeException()

@Serializable
sealed class UserExceptionInCommon(
    val msg: String,
    val help: String
) : UserException() {
    override val message = "$msg.\n$help"
}

@Serializable
sealed class UserExceptionInClient(
    val msg: String,
    val help: String
) : UserException() {
    override val message = "$msg.\n$help"
}

@Serializable
sealed class UserExceptionInWorker(
    val msg: String,
    val help: String
) : UserException() {
    override val message = "$msg.\n$help"
}

/***********************
 * Exceptions in common
 ***********************/

@Serializable
data class MissingMetaJavaClassDuringDeserialization(
    val data: SerializedData
) : UserExceptionInCommon(
    msg = "Trying to deserialize data without explicitly providing java class in \"${SerializedData.META_JAVA_CLASS}\" meta value",
    help = "You are probably trying to deserialize data generated by non-Java code. " +
        "Update this non-java code to include a \"${SerializedData.META_JAVA_CLASS}\" meta value " +
        "describing the java class that should be used"
)

@Serializable
data class ClassNotFoundDuringDeserialization(
    val data: SerializedData,
    val name: String
) : UserExceptionInCommon(
    msg = "Trying to deserialize data into \"$name\$ (according to \"${SerializedData.META_JAVA_CLASS}\" meta value) but this class is unknown",
    help = "Please make sure to include the \"$name\" class in your code base."
)

@Serializable
data class SerializerNotFoundDuringDeserialization(
    val name: String
) : UserExceptionInCommon(
    msg = "Trying to deserialize data into \"$name\$ but this class has no serializer",
    help = "Your data was correctly serialized, so make sure you use the same code base everywhere"
)

@Serializable
data class ExceptionDuringKotlinDeserialization(
    val name: String,
    val causeString: String
) : UserExceptionInCommon(
    msg = "Trying to deserialize data into \"$name\$ but an error occurred during Kotlin deserialization: $causeString",
    help = "Please make sure your class \"$name\$ can be safely serialized/deserialized using kotlinx.serialization and avro4k"
)

@Serializable
data class ExceptionDuringJsonDeserialization(
    val name: String,
    val causeString: String
) : UserExceptionInCommon(
    msg = "Trying to deserialize data into \"$name\$ but an error occurred during json deserialization: $causeString",
    help = "Please make sure your class \"$name\$ can be safely serialized/deserialized in Json using FasterXML/jackson"
)

/***********************
 * Exceptions in client
 ***********************/

data class NoMethodCallAtDispatch(
    val name: String,
    val dispatch: String
) : UserExceptionInClient(
    msg = "You must use a method of \"$name\" when using \"$dispatch\" method",
    help = "Make sure to call one method of \"$name\" within the curly braces - example: client.$dispatch<Foo> { bar(*args) }"
)

data class MultipleMethodCallsAtDispatch(
    val name: String,
    val method1: String,
    val method2: String
) : UserExceptionInClient(
    msg = "Only one method of \"$name\" can be dispatched at a time. You can not call \"$method2\" method as you have already called \"$method1\"",
    help = "Make sure you call only one method of the provided interface within curly braces - multiple calls such as client.dispatch<FooInterface> { barMethod(*args); bazMethod(*args) } is forbidden"
)

data class ErrorDuringJsonSerializationOfParameter(
    val parameterName: String,
    val parameterValue: Any?,
    val parameterType: String,
    val methodName: String,
    val className: String
) : UserExceptionInClient(
    msg = "Error during Json serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

data class ErrorDuringJsonDeserializationOfParameter(
    val parameterName: String,
    val parameterValue: Any?,
    val parameterType: String,
    val methodName: String,
    val className: String
) : UserExceptionInClient(
    msg = "Error during Json de-serialization of parameter $parameterName of $className::$methodName with value $parameterValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, please consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

data class InconsistentJsonSerializationOfParameter(
    val parameterName: String,
    val parameterValue: Any?,
    val restoredValue: Any?,
    val parameterType: String,
    val methodName: String,
    val className: String
) : UserExceptionInClient(
    msg = "Serialization/Deserialization of parameter $parameterName of $className::$methodName is not consistent: value provided $parameterValue - value after serialization/deserialization $restoredValue",
    help = "We are using Jackson library per default to serialize/deserialize object through the ${Json::class.java.name} wrapper. If an exception is thrown during serialization or deserialization, consider those options:\n" +
        "- modifying $parameterType to make it serializable by ${Json::class.java.name}\n" +
        "- replacing $parameterType per simpler parameters in $className::$methodName\n"
)

/***********************
 * Exceptions in worker
 ***********************/

@Serializable
data class ErrorDuringInstantiation(
    val name: String
) : UserExceptionInWorker(
    msg = "Impossible to instantiate class \"$name\" using newInstance()",
    help = "Consider those options:\n" +
        "- adding an empty constructor to \"$name\"\n" +
        "- using \"register\" method to provide an instance that will be used associated to \"$name\""
)

@Serializable
data class ClassNotFoundDuringInstantiation(
    val name: String
) : UserExceptionInWorker(
    msg = "Impossible to find a Class associated to $name",
    help = "Use \"register\" method to provide an instance that will be used associated to $name"
)

@Serializable
data class NoMethodFoundWithParameterTypes(
    val klass: String,
    val method: String,
    val parameterTypes: List<String>
) : UserExceptionInWorker(
    msg = "No method \"$method(${ parameterTypes.joinToString() })\" found in \"$klass\" class",
    help = "Make sure parameter types are consistent with your method definition"
)

@Serializable
data class NoMethodFoundWithParameterCount(
    val klass: String,
    val method: String,
    val parameterCount: Int
) : UserExceptionInWorker(
    msg = "No method \"$method\" with $parameterCount parameters found in \"$klass\" class",
    help = ""
)

@Serializable
data class TooManyMethodsFoundWithParameterCount(
    val klass: String,
    val method: String,
    val parameterCount: Int
) : UserExceptionInWorker(
    msg = "Unable to decide which method \"$method\" with $parameterCount parameters to use in \"$klass\" class",
    help = ""
)

@Serializable
data class RetryDelayHasWrongReturnType(
    val klass: String,
    val actualType: String,
    val expectedType: String
) : UserExceptionInWorker(
    msg = "In \"$klass\" class, method ${Constants.DELAY_BEFORE_RETRY_METHOD} returns a $actualType, it must be a $expectedType",
    help = "Please update your method definition to return a $expectedType (or null)"
)

@Serializable
data class ProcessingTimeout(
    val klass: String,
    val delay: Float
) : UserExceptionInWorker(
    msg = "The processing of task \"$klass\" took more than $delay seconds",
    help = "You can increase (or remove entirely) this constraint in the options ${TaskOptions::javaClass.name}"
)

@Serializable
data class ExceptionDuringParametersDeserialization(
    val taskName: String,
    val methodName: String,
    val input: List<SerializedData>,
    val parameterTypes: List<String>
) : UserExceptionInWorker(
    msg = "Impossible to deserialize input \"$input\" with types \"$parameterTypes\" for method \"$taskName::$methodName\"",
    help = ""
)

@Serializable
data class CanNotUseJavaReservedKeywordInMeta(
    val reserved: String
) : UserExceptionInWorker(
    msg = "\"$reserved\" is as reserved keyword, you can not use it in your meta data",
    help = ""
)
