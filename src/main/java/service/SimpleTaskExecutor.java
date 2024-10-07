package service;

import exception.SimpleAsyncException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import model.BackoffConfig;
import model.Task;
import model.TaskResult;
import model.TimeoutTask;
import pub.fi.ThrowableConsumer;
import pub.fi.ThrowableSupplier;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@Log4j2
public class SimpleTaskExecutor implements TaskExecutor {
	private final ExecutorService executorService;

	@Override
	public <T> TaskResult<T> execute(TimeoutTask<T> timeoutTask) {
		Instant start = timeoutTask.task().getStart();
		TaskExecutionDetail<T> taskExecutionDetail = TaskExecutionDetail.getInitial(timeoutTask, null);
		AtomicReference<String> threadName = new AtomicReference<>(Thread.currentThread().getName());
		AtomicInteger retryCount = new AtomicInteger(0);
		AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
		try {
			AsyncResult<T> asyncResult = getAsync(taskExecutionDetail, threadName, retryCount,exceptionRef);
			T t = getResult(timeoutTask, asyncResult.futureResult());
			if (t != null || exceptionRef.get() == null) {
				return TaskResult.success(t, start, Instant.now(), threadName.get(), timeoutTask.task().getTaskId(), retryCount);
			} else {
				return TaskResult.failure(exceptionRef.get(), start, Instant.now(), threadName.get(), timeoutTask.task().getTaskId(), retryCount);
			}
		} catch (Exception e) {
			return TaskResult.failure(e, start, Instant.now(), threadName.get(), timeoutTask.task().getTaskId(), retryCount);
		}
	}

	private <T> AsyncResult<T> getAsync(TaskExecutionDetail<T> taskExecutionDetail,
	                                    AtomicReference<String> threadName,
	                                    AtomicInteger retries,
	                                    AtomicReference<Exception> exceptionRef
	) {
		String taskId = taskExecutionDetail.task().getTaskId();
		CompletableFuture<T> completableResult = CompletableFuture.supplyAsync(() -> {
			try {
				threadName.set(Thread.currentThread().getName());
				AtomicBoolean stop = taskExecutionDetail.stop();
				AtomicInteger retryAttempts = taskExecutionDetail.retries();
				int maxRetries = taskExecutionDetail.maxRetries();
				Task<T> task = taskExecutionDetail.task();
				BackoffConfig backoffConfig = taskExecutionDetail.backoffConfig();
				boolean hasBackoff = backoffConfig != null;
				if (hasBackoff) {
					Thread.sleep(backoffConfig.initialDelay());
				}
				while (!stop.get() && retryAttempts.get() < maxRetries) {
					ThrowableSupplier<T> taskLogic = task.getTaskLogic();
					try {
						T t = taskLogic.get(); // what if there is an exception here
						if (Objects.nonNull(t)) {
							stop.set(true);
							retryAttempts.incrementAndGet();
							retries.set(retryAttempts.get());
							return t;
						} else {
							retryAttempts.incrementAndGet();
							retries.set(retryAttempts.get());
							if (hasBackoff) {
								try {
									long backoffDelay = backoffConfig.calculateRandomBackoff(retryAttempts.get());
									log.info("Task: {} failed to get value, retry the {}th time in: {}ms", task.getTaskId(), retryAttempts.get(), backoffDelay);
									Thread.sleep(backoffDelay);
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
									stop.set(true);
									throw new SimpleAsyncException(e);
								}
							}
						}
					} catch (Exception e) {
						retryAttempts.incrementAndGet();
						retries.set(retryAttempts.get());
						exceptionRef.set(e);
						if (hasBackoff && isRecoverable(e)) {
							try {
								long backoffDelay = backoffConfig.calculateRandomBackoff(retryAttempts.get());
								log.info("Task: {} failed to get value, retry the {}th time in: {}ms", task.getTaskId(), retryAttempts.get(), backoffDelay);
								Thread.sleep(backoffDelay);
							} catch (InterruptedException interruptedException) {
								Thread.currentThread().interrupt();
								stop.set(true);
								throw new SimpleAsyncException(e);
							}
						}
					}
				}
				return null;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				taskExecutionDetail.stop().set(true);
				throw new SimpleAsyncException(e);
			} finally {
				taskExecutionDetail.stop().set(true);
			}
		}, executorService);
		return new AsyncResult<>(completableResult, taskId, threadName.get(), retries.get());
	}

	private boolean isRecoverable(Exception e) {
		if (e instanceof TimeoutException) {
			return true;
		}
		if (e instanceof IOException) {
			return true;
		}
		if (e instanceof SQLException) {
			return true;
		}
		if (e instanceof InterruptedException) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	@Override
	public <T> TaskResult<T> executeBackoff(TimeoutTask<T> timeoutTask, BackoffConfig backoffConfig) {
		Instant start = Instant.now();
		TaskExecutionDetail<T> taskExecutionDetail = TaskExecutionDetail.getInitial(timeoutTask, backoffConfig);
		AtomicReference<String> threadName = new AtomicReference<>(Thread.currentThread().getName());
		AtomicInteger retryCount = new AtomicInteger(0);
		AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
		try {
			AsyncResult<T> asyncResult = getAsync(taskExecutionDetail, threadName, retryCount, exceptionRef);
			T result = getResult(timeoutTask, asyncResult.futureResult());
			if (result != null || exceptionRef.get() == null) {
				return TaskResult.success(result, start, Instant.now(), timeoutTask.task().getTaskId(), threadName.get(), retryCount);
			} else {
				return TaskResult.failure(exceptionRef.get(), start, Instant.now(), timeoutTask.task().getTaskId(), threadName.get(), retryCount);
			}
		} catch (Exception e) {
			return TaskResult.failure(e, start, Instant.now(), timeoutTask.task().getTaskId(), threadName.get(), retryCount);
		}
	}

	@Override
	public <T> void executeAsync(TimeoutTask<T> timeoutTask, ThrowableConsumer<T> resultHandler) {
		TaskExecutionDetail<T> taskExecutionDetail = TaskExecutionDetail.getInitial(timeoutTask);
		AtomicReference<String> threadName = new AtomicReference<>(Thread.currentThread().getName());
		AtomicInteger retryCount = new AtomicInteger(0);
		AtomicReference<Exception> exceptionRef = new AtomicReference<>(null);
		AsyncResult<T> asyncResult = getAsync(taskExecutionDetail, threadName, retryCount, exceptionRef);
		handleResultAsync(timeoutTask, asyncResult.futureResult(), resultHandler);
	}


	private <T> T getResult(TimeoutTask<T> timeoutTask, CompletableFuture<T> completableFuture) {
		if (timeoutTask.timeout() != -1) {
			try {
				return completableFuture.get(timeoutTask.timeout(), timeoutTask.timeUnit());
			} catch (Exception e) {
				throw new SimpleAsyncException(e);
			}
		} else {
			try {
				return completableFuture.get();
			} catch (Exception e) {
				throw new SimpleAsyncException(e);
			}
		}
	}


	private <T> void handleResultAsync(TimeoutTask<T> timeoutTask, CompletableFuture<T> completableFuture, ThrowableConsumer<T> resultHandler) {
		Task<T> task = timeoutTask.task();
		Instant start = task.getStart();
		completableFuture.whenCompleteAsync((result, exception) -> {
			if (exception != null) {
				TaskResult<T> errorTaskResult = TaskResult.failure(exception, start, Instant.now(), Thread.currentThread().getName(), task.getTaskId(), new AtomicInteger(0));
				resultHandler.onError(errorTaskResult);
			} else {
				try {
					resultHandler.accept(result);
				} catch (Exception e) {
					TaskResult<T> errorTaskResult = TaskResult.failure(e, start, Instant.now(), Thread.currentThread().getName(), task.getTaskId(), new AtomicInteger(0));
					resultHandler.onError(errorTaskResult);
				}
			}
		});
	}

	private record TaskExecutionDetail<T>(Instant start, int maxRetries, Task<T> task, AtomicBoolean stop,
	                                      AtomicInteger retries, AtomicReference<String> threadName,
	                                      BackoffConfig backoffConfig) {
		static <T> TaskExecutionDetail<T> getInitial(TimeoutTask<T> timeoutTask, @Nullable BackoffConfig backoffConfig) {
			Task<T> t = timeoutTask.task();
			int maxRetries = t.getMaxRetries();
			AtomicBoolean stop = new AtomicBoolean(false);
			AtomicInteger retries = new AtomicInteger(0);
			AtomicReference<String> threadName = new AtomicReference<>(Thread.currentThread().getName());
			return new TaskExecutionDetail<>(Instant.now(), maxRetries, t, stop, retries, threadName, backoffConfig);
		}

		static <T> TaskExecutionDetail<T> getInitial(TimeoutTask<T> timeoutTask) {
			return getInitial(timeoutTask, null);
		}
	}

	private record AsyncResult<T>(CompletableFuture<T> futureResult, String taskId, String threadName, int retryCount) {

	}

}
