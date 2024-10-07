package pub;

import lombok.RequiredArgsConstructor;
import model.BackoffConfig;
import model.Task;
import model.TaskResult;
import model.TimeoutTask;
import pub.fi.ThrowableConsumer;
import pub.fi.ThrowableSupplier;
import service.TaskExecutor;
import util.AssertionUtils;
import util.GeneratorUtils;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@RequiredArgsConstructor
public class SimpleAsyncHandler implements AsyncHandler {
	private static final int RANDOM_TASK_ID_LEN = 6;
	private final TaskExecutor taskExecutor;

	@Override
	public <T> void awaitAsync(ThrowableSupplier<T> task, ThrowableConsumer<T> resultHandler) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.nonNullOrThrow(resultHandler);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, -1, null, 1);
		consumeResult(taskExecutor::executeAsync, timeoutTask, resultHandler);
	}

	@Override
	public <T> T await(ThrowableSupplier<T> task, T defValOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, -1, null, 1);
		return getResult(taskExecutor::execute, timeoutTask, defValOnFailure);
	}

	@Override
	public <T> T awaitWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis, T defValOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(timeoutMillis);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, timeoutMillis, TimeUnit.MILLISECONDS, 1);
		return getResult(taskExecutor::execute, timeoutTask, defValOnFailure);
	}

	@Override
	public <T> T awaitWithRetries(ThrowableSupplier<T> task, Integer retryCount, T defValueOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(retryCount);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, -1, null, retryCount);
		return getResult(taskExecutor::execute, timeoutTask, defValueOnFailure);
	}

	@Override
	public <T> T awaitWithTimeoutRetries(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(timeoutMillis);
		AssertionUtils.isPositiveInteger(retryCount);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, timeoutMillis, TimeUnit.MILLISECONDS, retryCount);
		return getResult(taskExecutor::execute, timeoutTask, defValOnFailure);
	}

	@Override
	public <T> T awaitBackoff(ThrowableSupplier<T> task, Integer retryCount, T defValOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(retryCount);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, -1, null, retryCount);
		return getResult(taskExecutor::executeBackoff, timeoutTask, BackoffConfig.withDefault(), defValOnFailure);
	}

	@Override
	public <T> T awaitBackoffWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(timeoutMillis);
		AssertionUtils.isPositiveInteger(retryCount);
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, timeoutMillis, TimeUnit.MILLISECONDS, retryCount);
		return getResult(taskExecutor::executeBackoff, timeoutTask, BackoffConfig.withDefault(), defValOnFailure);
	}

	@Override
	public <T> T awaitBackoffWithBackoffConfig(ThrowableSupplier<T> task, Integer retryCount, T defValOnFailure, BackoffConfig backoffConfig) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(retryCount);
		AssertionUtils.nonNullOrThrow(backoffConfig, "Backoff Config must not be null or using the method without required backoff config");
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, -1, null, retryCount);
		return getResult(taskExecutor::executeBackoff, timeoutTask, backoffConfig, defValOnFailure);
	}

	@Override
	public <T> T awaitBackoffWithTimeoutBackoffConfig(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure, BackoffConfig backoffConfig) {
		AssertionUtils.nonNullOrThrow(task);
		AssertionUtils.isPositiveInteger(timeoutMillis);
		AssertionUtils.isPositiveInteger(retryCount);
		AssertionUtils.nonNullOrThrow(backoffConfig, "Backoff Config must not be null or using the method without required backoff config");
		TimeoutTask<T> timeoutTask = getTimeoutTask(task, timeoutMillis, TimeUnit.MILLISECONDS, retryCount);
		return getResult(taskExecutor::executeBackoff, timeoutTask, backoffConfig, defValOnFailure);
	}

	private <T> TimeoutTask<T> getTimeoutTask(ThrowableSupplier<T> task, long timeoutMillis, TimeUnit timeUnit, int maxRetry) {
		Task<T> t = getTask(task, maxRetry);
		return new TimeoutTask<>(t, timeoutMillis, timeUnit);
	}

	private <T> Task<T> getTask(ThrowableSupplier<T> task, int maxRetries) {
		Instant start = Instant.now();
		String taskId = GeneratorUtils.getRandomString(RANDOM_TASK_ID_LEN);
		return new Task<>(start, taskId, task, maxRetries);
	}

	private <T> void consumeResult(BiConsumer<TimeoutTask<T>, ThrowableConsumer<T>> biConsumer, TimeoutTask<T> timeoutTask, ThrowableConsumer<T> resultHandler) {
		biConsumer.accept(timeoutTask, resultHandler);
	}

	private <T> T getResult(Function<TimeoutTask<T>, TaskResult<T>> function, TimeoutTask<T> task, T defaultVal) {
		TaskResult<T> result = function.apply(task);
		return result.getResult().isPresent() ? result.getResult().get() : defaultVal;
	}

	private <T, E> T getResult(BiFunction<TimeoutTask<T>, E, TaskResult<T>> function, TimeoutTask<T> task, E e, T defaultVal) {
		TaskResult<T> result = function.apply(task, e);
		return result.getResult().isPresent() ? result.getResult().get() : defaultVal;
	}

}
