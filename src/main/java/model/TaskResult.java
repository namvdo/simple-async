package model;

import lombok.Getter;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class representing the result of an operation in a multi-threading and asynchronous environment.
 * It stores the result, any exceptions encountered, and relevant metadata for traceability.
 *
 * @param <T> The type of result that the task produces.
 */
@Getter
public class TaskResult<T> {
	private final T result;
	private final Throwable exception;
	private final Instant startTime;
	private final Instant endTime;
	private final String threadName;
	private final String taskId;
	private final AtomicInteger retryCount;

	private TaskResult(T result, Throwable exception, Instant startTime, Instant endTime, String threadName, String taskId, AtomicInteger retryCount) {
		this.result = result;
		this.exception = exception;
		this.startTime = startTime;
		this.endTime = endTime;
		this.threadName = threadName;
		this.taskId = taskId;
		this.retryCount = retryCount;
	}

	/**
	 * Static factory method to create a successful model.TaskResult.
	 *
	 * @param result The result of the successful task.
	 * @param startTime The start time of the task.
	 * @param endTime The end time of the task.
	 * @param threadName The name of the thread on which the task ran.
	 * @param taskId The ID of the task for traceability.
	 * @param retryCount The number of retries attempted before success.
	 * @param <T> The type of result.
	 * @return A successful model.TaskResult.
	 */
	public static <T> TaskResult<T> success(T result, Instant startTime, Instant endTime, String threadName, String taskId, AtomicInteger retryCount) {
		return new TaskResult<>(result, null, startTime, endTime, threadName, taskId, retryCount);
	}

	/**
	 * Static factory method to create a failed model.TaskResult.
	 *
	 * @param exception The at.cpx.exception that occurred during task execution.
	 * @param startTime The start time of the task.
	 * @param endTime The end time of the task.
	 * @param threadName The name of the thread on which the task ran.
	 * @param taskId The ID of the task for traceability.
	 * @param retryCount The number of retries attempted before failure.
	 * @param <T> The type of result (which will be null in case of failure).
	 * @return A failed model.TaskResult.
	 */
	public static <T> TaskResult<T> failure(Throwable exception, Instant startTime, Instant endTime, String threadName, String taskId, AtomicInteger retryCount) {
		return new TaskResult<>(null, exception, startTime, endTime, threadName, taskId, retryCount);
	}

	/**
	 * @return The result of the task, if it was successful.
	 */
	public Optional<T> getResult() {
		return Optional.ofNullable(result);
	}

	public TaskResult<T> increaseRetryAndGet() {
		retryCount.incrementAndGet();
		if (this.result == null) {
			return failure(null, this.startTime, this.endTime, this.threadName, this.taskId, retryCount);
		} else {
			return success(this.result, this.startTime, this.endTime, this.threadName, this.taskId, retryCount);
		}
	}

	public TaskResult<T> successAndIncreaseRetryCount(T result, Instant start, Instant finish, String threadName, String taskId) {
		return null;
	}

	public T getResultOrDefault(T defaultVal) {
		return result == null ? defaultVal : result;
	}

	/**
	 * @return The at.cpx.exception encountered during task execution, if any.
	 */
	public Optional<Throwable> getException() {
		return Optional.ofNullable(exception);
	}


}