package pub;

import pub.fi.ThrowableConsumer;
import pub.fi.ThrowableSupplier;
import model.BackoffConfig;
import service.SimpleTaskExecutor;
import service.TaskExecutor;

import java.util.Optional;
import java.util.concurrent.*;

/**
 * The {@code AsyncHandler} interface provides a set of simple APIs for handling asynchronous tasks with support
 * for retries, timeouts, and backoff strategies. This interface is useful for scenarios where tasks can fail,
 * and you want to handle failures gracefully, either by retrying the task, using a timeout, or applying a backoff
 * strategy between retries.
 * Example Usage:
 * <pre>{@code
 * AsyncHandler asyncHandler = AsyncHandler.withDefault();
 * // Example with a simple result
 * String result = asyncHandler.await(() -> {
 *     // Some task that might throw an exception
 *     return "Hello, world!";
 * }, "Default Value");
 * System.out.println(result);
 *
 * // Retry a task 3 times with a timeout of 5 seconds using the default backoff configuration
 * Optional<String> backOffResult = asyncHandler.awaitBackoffWithTimeout(() -> {
 *    return someLongRunningOperation();
 * }, 5000, 3); // timeout 5 seconds at at most 3 retries
 * System.out.println(backOffResult.orElse(null));
 *
 * // Call awaitResult with a task that may fail, timeout, and retry count
 * String result = asyncHandler.awaitWithTimeoutRetries(() -> {
 *     return getSomeValueFromDb();
 * }, 1000, 5, "Default Value on Failure"); // 1 second timeout and 5 retries
 * }</pre>
 */

public interface AsyncHandler {
    /**
     * Executes the provided {@code task} asynchronously which doesn't block the caller thread and handles its result through the provided {@code resultHandler}.
     * The {@code resultHandler} is a callback and called when the task completes successfully or with an error.
     *
     * @param task            the task to execute asynchronously
     * @param resultHandler the handler that consumes the result or handles the error
     * @param <T>             the type of the task result
     *
     * <p>
     * Example Usage:
     * <pre>{@code
     * // Define the task
     * ThrowableSupplier<String> task = () -> "Hello, Async World!";
     * // Define the callback to handle the result
     * ThrowableConsumer<String> resultHandler = result -> System.out.println("Result: " + result);
     * // Execute the task asynchronously
     * asyncHandler.awaitAsync(task, resultHandler);
     * }</pre>
     */
    <T> void awaitAsync(ThrowableSupplier<T> task, ThrowableConsumer<T> resultHandler);

    /**
     * Executes the provided {@code task} and returns its result. If the task fails (throws an exception),
     * it returns the specified {@code defValOnFailure}.
     *
     * @param task            the task to execute
     * @param defValOnFailure the default value to return if the task fails
     * @param <T>            the type of the result
     * @return the result of the task, or {@code defValOnFailure} if the task fails
     * @throws RuntimeException if execution fails
     * <p>
     * Example Usage:
     * <pre>{@code
     * String result = asyncHandler.await(() -> {
     *     return someDatabaseCall();
     * }, "Default");
     * }</pre>
     */
    <T> T await(ThrowableSupplier<T> task, T defValOnFailure);

    /**
     * Executes the provided {@code task} with a timeout. If the task exceeds the specified {@code timeoutMillis}
     * or fails, it returns the specified {@code defValOnFailure}.
     *
     * @param task            the task to execute
     * @param timeoutMillis   the maximum time to wait for the task to complete, in milliseconds
     * @param defValOnFailure the default value to return if the task fails or times out
     * @param <T>            the type of the result
     * @return the result of the task, or {@code defValOnFailure} if the task fails or times out
     * <p>
     * Example Usage:
     * <pre>{@code
     * String result = asyncHandler.awaitWithTimeout(() -> {
     *     return someDatabaseCall();
     * }, 1000L, "Default");
     * }</pre>
     */
    <T> T awaitWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis, T defValOnFailure);

    /**
     * Executes the provided {@code task} with a retry mechanism. The task will be retried the specified number of times
     * if it fails. After exceeding the retries, it returns the specified {@code defValOnFailure}.
     *
     * @param task            the task to execute
     * @param retryCount      the number of retry attempts
     * @param defValOnFailure the default value to return if the task fails after all retries
     * @param <T>            the type of the result
     * @return the result of the task, or {@code defValOnFailure} if the task fails after retries
     * <p>
     * Example Usage:
     * <pre>{@code
     * String result = asyncHandler.awaitWithRetries(() -> {
     *     return someUnreliableCall();
     * }, 3, "Default");
     * }</pre>
     */
    <T> T awaitWithRetries(ThrowableSupplier<T> task, Integer retryCount, T defValOnFailure);

    /**
     * Executes the provided {@code task} with both a timeout and retry mechanism. If the task exceeds the timeout or fails
     * after the specified number of retries, it returns the specified {@code defValOnFailure}.
     *
     * @param task            the task to execute
     * @param timeoutMillis    the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount       the number of retry attempts
     * @param defValOnFailure  the default value to return if the task fails after retries or times out
     * @param <T>              the type of the result
     * @return the result of the task, or {@code defValOnFailure} if the task fails after retries or times out
     * <p>
     * Example Usage:
     * <pre>{@code
     * String result = asyncHandler.awaitWithTimeoutRetries(() -> {
     *     return someExpensiveCall();
     * }, 5000, 3, "Default");
     * }</pre>
     */
    <T> T awaitWithTimeoutRetries(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure);

    /**
     * Executes the provided {@code task} and returns an {@link Optional} wrapping the result.
     * If the task fails, an empty {@link Optional} is returned.
     *
     * @param task  the task to execute
     * @param <T>   the type of the result
     * @return an {@link Optional} containing the result, or empty if the task fails
     */
    default <T> Optional<T> awaitNullable(ThrowableSupplier<T> task) {
        return Optional.ofNullable(await(task, null));
    }

    /**
     * Executes the provided {@code task} with a timeout and returns an {@link Optional} wrapping the result.
     * If the task fails or times out, an empty {@link Optional} is returned.
     *
     * @param task          the task to execute
     * @param timeoutMillis the maximum time to wait for the task to complete, in milliseconds
     * @param <T>           the type of the result
     * @return an {@link Optional} containing the result, or empty if the task fails or times out
     */
    default <T> Optional<T> awaitNullableWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis) {
        return Optional.ofNullable(awaitWithTimeout(task, timeoutMillis, null));
    }

    /**
     * Executes the provided {@code task} with a retry mechanism and returns an {@link Optional} wrapping the result.
     * If the task fails after retries, an empty {@link Optional} is returned.
     *
     * @param task       the task to execute
     * @param retryCount the number of retry attempts
     * @param <T>        the type of the result
     * @return an {@link Optional} containing the result, or empty if the task fails after retries
     */
    default <T> Optional<T> awaitNullableWithRetries(ThrowableSupplier<T> task, Integer retryCount) {
        return Optional.ofNullable(awaitWithRetries(task, retryCount, null));
    }

    /**
     * Executes the provided {@code task} with both a timeout and retry mechanism, returning an {@link Optional}.
     * If the task fails after retries or times out, an empty {@link Optional} is returned.
     *
     * @param task          the task to execute
     * @param timeoutMillis the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount    the number of retry attempts
     * @param <T>           the type of the result
     * @return an {@link Optional} containing the result, or empty if the task fails after retries or times out
     */
    default <T> Optional<T> awaitNullableWithTimeoutRetries(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount) {
        return Optional.ofNullable(awaitWithTimeoutRetries(task, timeoutMillis, retryCount, null));
    }

    /**
     * Executes the provided {@code task} with a retry mechanism using a simple backoff strategy. The task will be retried
     * the specified number of times if it fails (throws an exception). If the task still fails after all retries,
     * the specified {@code defValOnFailure} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param retryCount       the number of times to retry the task in case of failure
     * @param defValOnFailure  the default value to return if the task fails after all retries
     * @param <T>              the type of the result expected from the task
     * @return the result of the task, or {@code defValOnFailure} if the task fails after all retries
     * Example Usage:
     * <pre>{@code
     * // Retry a network call 3 times before returning a default value
     * String result = asyncHandler.awaitBackoff(() -> {
     *     return someNetworkCall();
     * }, 3, "Default Value");
     *
     * System.out.println(result);
     * }</pre>
     */
    <T> T awaitBackoff(ThrowableSupplier<T> task, Integer retryCount, T defValOnFailure);

    /**
     * Executes the provided {@code task} with both a timeout and retry mechanism. The task will be retried the specified
     * number of times if it fails (throws an exception) and will time out if it exceeds the specified {@code timeoutMillis}.
     * If the task fails after all retries or times out, the specified {@code defValOnFailure} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param timeoutMillis    the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount       the number of times to retry the task in case of failure
     * @param defValOnFailure  the default value to return if the task fails after all retries or exceeds the timeout
     * @param <T>              the type of the result expected from the task
     * @return the result of the task, or {@code defValOnFailure} if the task fails after retries or times out
     * Example Usage:
     * <pre>{@code
     * // Retry a task 3 times and set a timeout of 5 seconds
     * String result = asyncHandler.awaitBackoffWithTimeout(() -> {
     *     return someLongRunningTask();
     * }, 5000, 3, "Default Value");
     *
     * System.out.println(result);  // Outputs the result of the task or "Default Value" on failure or timeout
     * }</pre>
     */
    <T> T awaitBackoffWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure);

    /**
     * Executes the provided {@code task} with a retry mechanism that applies a backoff strategy. The task will be retried
     * the specified number of times if it fails (throws an exception). The backoff strategy is defined by the provided
     * {@code backoffConfig}. If the task still fails after all retries, the specified {@code defValOnFailure} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param retryCount       the number of times to retry the task in case of failure
     * @param defValOnFailure  the default value to return if the task fails after all retries
     * @param backoffConfig    the configuration defining the backoff strategy, including parameters like initial delay,
     *                         multiplier, max delay, and jitter range
     * @param <T>              the type of the result expected from the task
     * @return the result of the task, or {@code defValOnFailure} if the task fails after all retries
     * Example Usage:
     * <pre>{@code
     * BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
     *
     * // Retry a task 3 times with exponential backoff
     * String result = asyncHandler.awaitBackoffWithBackoffConfig(() -> {
     *     return someUnreliableOperation();
     * }, 3, "Default Value", backoffConfig);
     *
     * System.out.println(result);  // Outputs the result of the task or "Default Value" on failure
     * }</pre>
     */
    <T> T awaitBackoffWithBackoffConfig(ThrowableSupplier<T> task, Integer retryCount, T defValOnFailure, BackoffConfig backoffConfig);

    /**
     * Executes the provided {@code task} with both a timeout and retry mechanism that applies a backoff strategy.
     * The task will be retried the specified number of times if it fails (throws an exception) and will time out
     * if it exceeds the specified {@code timeoutMillis}. If the task fails after all retries or times out,
     * the specified {@code defValOnFailure} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param timeoutMillis    the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount       the number of times to retry the task in case of failure
     * @param defValOnFailure  the default value to return if the task fails after all retries or exceeds the timeout
     * @param backoffConfig    the configuration defining the backoff strategy, including parameters like initial delay,
     *                         multiplier, max delay, and jitter range
     * @param <T>              the type of the result expected from the task
     * @return the result of the task, or {@code defValOnFailure} if the task fails after all retries or times out
     * Example Usage:
     * <pre>{@code
     * BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
     *
     * // Retry a task 3 times with exponential backoff and a timeout of 5 seconds
     * String result = asyncHandler.awaitBackoffWithTimeoutBackoffConfig(() -> {
     *     return someLongRunningOperation();
     * }, 5000, 3, "Default Value", backoffConfig);
     *
     * System.out.println(result);  // Outputs the result of the task or "Default Value" on failure or timeout
     * }</pre>
     */
    <T> T awaitBackoffWithTimeoutBackoffConfig(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, T defValOnFailure, BackoffConfig backoffConfig);

    /**
     * Executes the provided {@code task} with a retry mechanism that applies a backoff strategy. The task will be retried
     * the specified number of times if it fails (throws an exception). If the task still fails after all retries,
     * an empty {@link Optional} is returned.
     *
     * @param task         the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param retryCount   the number of times to retry the task in case of failure
     * @param <T>          the type of the result expected from the task
     * @return an {@link Optional} containing the result of the task if successful;
     *         otherwise, an empty {@link Optional} if the task fails after all retries
     * Example Usage:
     * <pre>{@code
     * // Retry a task 3 times and return an Optional result
     * Optional<String> result = asyncHandler.awaitNullableBackoff(() -> {
     *     return someUnreliableOperation();
     * }, 3);
     *
     * result.ifPresentOrElse(
     *     value -> System.out.println("Success: " + value),
     *     () -> System.out.println("Operation failed after retries.")
     * );
     * }</pre>
     */
    default <T> Optional<T> awaitNullableBackoff(ThrowableSupplier<T> task, Integer retryCount) {
        return Optional.ofNullable(awaitBackoff(task, retryCount, (T) null));
    }

    /**
     * Executes the provided {@code task} with both a timeout and a retry mechanism that applies a backoff strategy.
     * The task will be retried the specified number of times if it fails (throws an exception) and will time out
     * if it exceeds the specified {@code timeoutMillis}. If the task fails after all retries or times out,
     * an empty {@link Optional} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param timeoutMillis    the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount       the number of times to retry the task in case of failure
     * @param <T>              the type of the result expected from the task
     * @return an {@link Optional} containing the result of the task if successful;
     *         otherwise, an empty {@link Optional} if the task fails after all retries or times out
     *
     * Example Usage:
     * <pre>{@code
     * // Retry a task 3 times with a timeout of 5 seconds and return an Optional result
     * Optional<String> result = asyncHandler.awaitNullableBackoffWithTimeout(() -> {
     *     return someLongRunningOperation();
     * }, 5000, 3);
     *
     * result.ifPresentOrElse(
     *     value -> System.out.println("Success: " + value),
     *     () -> System.out.println("Operation failed after retries or timed out.")
     * );
     * }</pre>
     */
    default <T> Optional<T> awaitNullableBackoffWithTimeout(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount) {
        return Optional.ofNullable(awaitBackoffWithTimeout(task, timeoutMillis, retryCount, (T) null));
    }

    /**
     * Executes the provided {@code task} with a retry mechanism that applies a backoff strategy.
     * The task will be retried the specified number of times if it fails (throws an exception).
     * If the task fails after all retries, an empty {@link Optional} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param retryCount       the number of times to retry the task in case of failure
     * @param backoffConfig    the configuration defining the backoff strategy, including parameters like initial delay,
     *                         multiplier, max delay, and jitter range
     * @param <T>              the type of the result expected from the task
     * @return an {@link Optional} containing the result of the task if successful;
     *         otherwise, an empty {@link Optional} if the task fails after all retries
     * Example Usage:
     * <pre>{@code
     * BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
     * // Retry a task 3 times with exponential backoff
     * Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(() -> {
     *     return someUnreliableOperation();
     * }, 3, backoffConfig);
     *
     * result.ifPresentOrElse(
     *     value -> System.out.println("Success: " + value),
     *     () -> System.out.println("Operation failed after retries.")
     * );
     * }</pre>
     */
    default <T> Optional<T> awaitNullableBackoffWithBackoffConfig(ThrowableSupplier<T> task, Integer retryCount, BackoffConfig backoffConfig) {
        return Optional.ofNullable(awaitBackoffWithBackoffConfig(task, retryCount, null, backoffConfig));
    }

    /**
     * Executes the provided {@code task} with both a timeout and a retry mechanism that applies a backoff strategy.
     * The task will be retried the specified number of times if it fails (throws an exception) and will time out
     * if it exceeds the specified {@code timeoutMillis}. If the task fails after all retries or times out,
     * an empty {@link Optional} is returned.
     *
     * @param task             the task to execute, represented by a {@link ThrowableSupplier}, which can throw an exception
     * @param timeoutMillis    the maximum time to wait for the task to complete, in milliseconds
     * @param retryCount       the number of times to retry the task in case of failure
     * @param backoffConfig    the configuration defining the backoff strategy, including parameters like initial delay,
     *                         multiplier, max delay, and jitter range
     * @param <T>              the type of the result expected from the task
     * @return an {@link Optional} containing the result of the task if successful;
     *         otherwise, an empty {@link Optional} if the task fails after all retries or times out
     *
     * Example Usage:
     * <pre>{@code
     * BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
     * // Retry a task 3 times with a timeout of 5 seconds and return an Optional result
     * Optional<String> result = asyncHandler.awaitNullableBackoffWithTimeoutBackoffConfig(() -> {
     *     return someLongRunningOperation();
     * }, 5000, 3, backoffConfig);
     *
     * result.ifPresentOrElse(
     *     value -> System.out.println("Success: " + value),
     *     () -> System.out.println("Operation failed after retries or timed out.")
     * );
     * }</pre>
     */
    default <T> Optional<T> awaitNullableBackoffWithTimeoutBackoffConfig(ThrowableSupplier<T> task, Long timeoutMillis, Integer retryCount, BackoffConfig backoffConfig) {
        return Optional.ofNullable(awaitBackoffWithTimeoutBackoffConfig(task, timeoutMillis, retryCount, null, backoffConfig));
    }

    final class DefaultAsyncHandlerInstanceHolder {
        static final ExecutorService executor = Executors.newFixedThreadPool(4);
        static final TaskExecutor taskExecutor = new SimpleTaskExecutor(executor);
        static final AsyncHandler INSTANCE = new SimpleAsyncHandler(taskExecutor);
    }
    /**
     * Creates a default {@code AsyncHandler} instance with a thread pool size of 4. If the default instance
     * is already created, it will be reused.
     * <p>
     * This method initializes an {@link ExecutorService} with a fixed thread pool
     * of 4 threads and uses it to create a {@link TaskExecutor} instance.
     * The resulting {@code AsyncHandler} can be used to execute tasks asynchronously
     * with default configurations.
     * </p>
     *
     * @return a new instance of {@code AsyncHandler} with a default thread pool size
     */
    static AsyncHandler withDefault() {
        return DefaultAsyncHandlerInstanceHolder.INSTANCE;
    }


    /**
     * Creates a new {@code AsyncHandler} instance with a specified pool size.
     * <p>
     * This method initializes an {@link ExecutorService} with a fixed thread pool
     * of the specified size and uses it to create a {@link TaskExecutor} instance.
     * The resulting {@code AsyncHandler} can be utilized for executing tasks asynchronously
     * based on the provided pool size.
     * </p>
     *
     * @param poolSize the number of threads in the thread pool
     * @return a new instance of {@code AsyncHandler} with the specified thread pool size
     */
    static AsyncHandler withPoolSize(int poolSize) {
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        TaskExecutor taskExecutor = new SimpleTaskExecutor(executorService);
        return new SimpleAsyncHandler(taskExecutor);
    }

    /**
     * Creates a new {@code AsyncHandler} instance with a cached thread pool size.
     * <p>
     * This method initializes a {@link ThreadPoolExecutor} that allows for an
     * unbounded number of threads, with a core pool size of 0 and a maximum pool size
     * defined by the specified {@code poolSize}. The executor will dynamically create
     * new threads as needed to handle tasks, allowing for efficient management of
     * resources when task demand is variable.
     * </p>
     *
     * @param poolSize the maximum number of threads in the thread pool
     * @return a new instance of {@code AsyncHandler} with a cached thread pool configuration
     */
    static AsyncHandler withCachedPoolSize(int poolSize) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                0,
                poolSize,
                60_000L,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>()
        );
        TaskExecutor taskExecutor = new SimpleTaskExecutor(threadPoolExecutor);
        return new SimpleAsyncHandler(taskExecutor);
    }

}
