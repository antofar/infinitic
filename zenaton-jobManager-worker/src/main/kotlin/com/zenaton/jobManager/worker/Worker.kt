package com.zenaton.jobManager.worker
import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.Constants
import com.zenaton.jobManager.common.data.JobAttemptContext
import com.zenaton.jobManager.common.data.JobAttemptError
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.jobManager.common.exceptions.ClassNotFoundDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.DelayBeforeRetryReturnTypeError
import com.zenaton.jobManager.common.exceptions.ErrorDuringJobInstantiation
import com.zenaton.jobManager.common.exceptions.InvalidUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.JobAttemptContextRetrievedOutsideOfProcessingThread
import com.zenaton.jobManager.common.exceptions.JobAttemptContextSetFromExistingProcessingThread
import com.zenaton.jobManager.common.exceptions.MultipleUseOfDividerInJobName
import com.zenaton.jobManager.common.exceptions.NoMethodFoundWithParameterCount
import com.zenaton.jobManager.common.exceptions.NoMethodFoundWithParameterTypes
import com.zenaton.jobManager.common.exceptions.ProcessingTimeout
import com.zenaton.jobManager.common.exceptions.TooManyMethodsFoundWithParameterCount
import com.zenaton.jobManager.common.messages.ForWorkerMessage
import com.zenaton.jobManager.common.messages.JobAttemptCompleted
import com.zenaton.jobManager.common.messages.JobAttemptFailed
import com.zenaton.jobManager.common.messages.JobAttemptStarted
import com.zenaton.jobManager.common.messages.RunJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

open class Worker {
    lateinit var dispatcher: Dispatcher
    companion object {

        private val registeredTasks = ConcurrentHashMap<String, Any>()

        // cf. https://www.baeldung.com/java-threadlocal
        private val contexts = ConcurrentHashMap<Long, JobAttemptContext>()

        /**
         * Use this method to register a task instance to use for a given name
         */
        fun register(name: String, job: Any) {
            if (name.contains(Constants.METHOD_DIVIDER)) throw InvalidUseOfDividerInJobName(name)

            registeredTasks[name] = job
        }

        /**
         * Use this method to unregister a given name (mostly used in tests)
         */
        fun unregister(name: String) {
            registeredTasks.remove(name)
        }

        /**
         * Use this method to retrieve JobAttemptContext associated to a task
         */
        fun getContext(): JobAttemptContext? {
            val key = getContextKey()
            if (! contexts.containsKey(key)) throw JobAttemptContextRetrievedOutsideOfProcessingThread()

            return contexts[key]
        }

        private fun getContextKey() = Thread.currentThread().id
    }

    fun handle(msg: ForWorkerMessage) {
        runBlocking {
            suspendingHandle(msg)
        }
    }

    suspend fun suspendingHandle(msg: ForWorkerMessage) {
        when (msg) {
            is RunJob -> {
                // let engine know that we are processing the message
                sendTaskStarted(msg)

                // trying to instantiate the task
                val (task, method, parameters, options) = try {
                    parse(msg)
                } catch (e: Exception) {
                    // returning the exception (no retry)
                    sendTaskFailed(msg, e, null)
                    // we stop here
                    return
                }

                val context = JobAttemptContext(
                    jobId = msg.jobId,
                    jobAttemptId = msg.jobAttemptId,
                    jobAttemptIndex = msg.jobAttemptIndex,
                    jobAttemptRetry = msg.jobAttemptRetry,
                    jobMeta = msg.jobMeta,
                    jobOptions = msg.jobOptions
                )

                // the method invocation is done through a coroutine
                // - to manage processing timeout
                // - to manage cancellation after timeout
                val parentJob = Job()
                CoroutineScope(Dispatchers.Default + parentJob).launch {
                    var contextKey = 0L

                    launch {
                        try {
                            // ensure that contextKey is linked to same thread than method.invoke below
                            contextKey = getContextKey()

                            // add context to static list
                            setContext(contextKey, context)

                            // running timeout delay if any
                            if (options.runningTimeout != null && options.runningTimeout!! > 0F) {
                                launch {
                                    delay((1000 * options.runningTimeout!!).toLong())
                                    // update context with the cause (to be potentially used in getRetryDelay method)
                                    context.exception = ProcessingTimeout(task.javaClass.name, options.runningTimeout!!)
                                    // returning a timeout
                                    getRetryDelayAndFailTask(task, msg, parentJob, contextKey, context)
                                }
                            }

                            val output = method.invoke(task, *parameters)

                            completeTask(msg, output, parentJob, contextKey)
                        } catch (e: InvocationTargetException) {
                            // update context with the cause (to be potentially used in getRetryDelay method)
                            context.exception = e.cause
                            // retrieve delay before retry
                            getRetryDelayAndFailTask(task, msg, parentJob, contextKey, context)
                        } catch (e: Exception) {
                            // returning the exception (no retry)
                            failTask(msg, e, null, parentJob, contextKey)
                        }
                    }
                }.join()
            }
        }
    }

    private fun setContext(key: Long, context: JobAttemptContext) {
        if (contexts.containsKey(key)) throw JobAttemptContextSetFromExistingProcessingThread()
        contexts[key] = context
    }

    private fun delContext(key: Long) {
        if (! contexts.containsKey(key)) throw JobAttemptContextSetFromExistingProcessingThread()
        contexts.remove(key)
    }

    private fun getRetryDelayAndFailTask(task: Any, msg: RunJob, parentJob: Job, contextKey: Long, context: JobAttemptContext) {
        when (val delay = getDelayBeforeRetry(task, context)) {
            is RetryDelayRetrieved -> {
                // returning the original cause
                failTask(msg, context.exception, delay.value, parentJob, contextKey)
            }
            is RetryDelayRetrievalException -> {
                // returning the error in getRetryDelay, without retry
                failTask(msg, delay.e, null, parentJob, contextKey)
            }
        }
    }

    private fun completeTask(msg: RunJob, output: Any?, parentJob: Job, contextKey: Long) {
        // returning output
        sendJobCompleted(msg, output)

        // removing context from static list
        delContext(contextKey)

        // make sure to close both coroutines
        parentJob.cancel()
    }

    private fun failTask(msg: RunJob, e: Throwable?, delay: Float?, parentJob: Job, contextKey: Long) {
        // returning throwable
        sendTaskFailed(msg, e, delay)

        // removing context from static list
        delContext(contextKey)

        // make sure to close both coroutines
        parentJob.cancel()
    }

    private fun parse(msg: RunJob): JobCommand {
        val (jobName, methodName) = getClassAndMethodNames(msg)
        val job = getTaskInstance(jobName)
        val parameterTypes = getMetaParameterTypes(msg)
        val method = getMethod(job, methodName, msg.jobInput.input.size, parameterTypes)
        val parameters = getParameters(msg.jobInput, parameterTypes ?: method.parameterTypes)

        return JobCommand(job, method, parameters, msg.jobOptions)
    }

    private fun getClassAndMethodNames(msg: RunJob): List<String> {
        val parts = msg.jobName.name.split(Constants.METHOD_DIVIDER)
        return when (parts.size) {
            1 -> parts + Constants.METHOD_DEFAULT
            2 -> parts
            else -> throw MultipleUseOfDividerInJobName(msg.jobName.name)
        }
    }

    private fun getTaskInstance(name: String): Any {
        // return registered instance if any
        if (registeredTasks.containsKey(name)) return registeredTasks[name]!!

        // if no instance is registered, try to instantiate this job
        val klass = getClass(name)

        return try {
            klass.newInstance()
        } catch (e: Exception) {
            throw ErrorDuringJobInstantiation(name)
        }
    }

    private fun getMetaParameterTypes(msg: RunJob) = msg.jobMeta.getParameterTypes()
        ?.map { getClass(it) }
        ?.toTypedArray()

    private fun getClass(name: String) = when (name) {
        "bytes" -> Byte::class.java
        "short" -> Short::class.java
        "int" -> Int::class.java
        "long" -> Long::class.java
        "float" -> Float::class.java
        "double" -> Double::class.java
        "boolean" -> Boolean::class.java
        "char" -> Character::class.java
        else ->
            try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                throw ClassNotFoundDuringJobInstantiation(name)
            }
    }

    private fun getMethod(job: Any, methodName: String, parameterCount: Int, parameterTypes: Array<Class<*>>?): Method {
        // Case where parameter types have been provided
        if (parameterTypes != null) return try {
            job::class.java.getMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            throw NoMethodFoundWithParameterTypes(job::class.java.name, methodName, parameterTypes.map { it.name })
        }

        // if not, hopefully there is only one method with this name
        val methods = job::class.javaObjectType.methods.filter { it.name == methodName && it.parameterCount == parameterCount }
        if (methods.isEmpty()) throw NoMethodFoundWithParameterCount(job::class.java.name, methodName, parameterCount)
        if (methods.size > 1) throw TooManyMethodsFoundWithParameterCount(job::class.java.name, methodName, parameterCount)

        return methods[0]
    }

    private fun getParameters(input: JobInput, parameterTypes: Array<Class<*>>): Array<Any?> {
        return input.input.mapIndexed {
            index, serializedData ->
            serializedData.deserialize(parameterTypes[index])
        }.toTypedArray()
    }

    private fun getDelayBeforeRetry(job: Any, context: JobAttemptContext): RetryDelayCommand {
        val method = try {
            job::class.java.getMethod(Constants.DELAY_BEFORE_RETRY_METHOD, JobAttemptContext::class.java)
        } catch (e: NoSuchMethodException) {
            return RetryDelayRetrieved(null)
        }

        val actualType = method.genericReturnType.typeName
        val expectedType = "float"
        if (actualType != expectedType) return RetryDelayRetrievalException(
            DelayBeforeRetryReturnTypeError(job::class.java.name, actualType, expectedType)
        )

        return try {
            RetryDelayRetrieved(method.invoke(job, context) as Float?)
        } catch (e: Exception) {
            RetryDelayRetrievalException(e)
        }
    }

    private fun sendTaskStarted(msg: RunJob) {
        val jobAttemptStarted = JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex
        )

        dispatcher.toJobEngine(jobAttemptStarted)
    }

    private fun sendTaskFailed(msg: RunJob, error: Throwable?, delay: Float? = null) {
        val jobAttemptFailed = JobAttemptFailed(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptDelayBeforeRetry = delay,
            jobAttemptError = JobAttemptError(error)
        )

        dispatcher.toJobEngine(jobAttemptFailed)
    }

    private fun sendJobCompleted(msg: RunJob, output: Any?) {
        val jobAttemptCompleted = JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobOutput = JobOutput(SerializedData.from(output))
        )

        dispatcher.toJobEngine(jobAttemptCompleted)
    }
}
