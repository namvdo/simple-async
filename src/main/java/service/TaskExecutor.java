package service;

import model.BackoffConfig;
import model.TaskResult;
import model.TimeoutTask;
import pub.fi.ThrowableConsumer;

public interface TaskExecutor {
    <T> TaskResult<T> execute(TimeoutTask<T> timeoutTask);
    <T> TaskResult<T> executeBackoff(TimeoutTask<T>  timeoutTask, BackoffConfig backoffConfig);
    <T> void executeAsync(TimeoutTask<T> timeoutTask, ThrowableConsumer<T> resultHandler);
}
