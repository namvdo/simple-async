### Overview
This library aims at providing functionality for handling common tasks in an efficient way through a set of simple APIs.

The public APIs placed in the `pub` package, currently implemented classes with functionality:
#### AsyncHandler - an interface for handling asynchronous operations

To get a value "Success" value or fallback to "Default" value after 2 seconds asynchronously, here is what we typically do:

```java
ExecutorService executorService = Executors.newFixedThreadPool(2);
CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
	return "Success";
}, executorService);
String result = "Default";
try {
    result = completableFuture.get(2000L, TimeUnit.MILLISECONDS);
} catch (InterruptedException e) {
} catch (ExecutionException e) {
} catch (TimeoutException e) {
}
System.out.println(result);
```
What we want to achieve is simple, but there are too much information we need to take care of in this case. What is an `ExecutorService`, 
what is `CompletetableFuture`, why there are so many different exceptions, and what should we do in case they occurred?

`AsyncHandler` gives you a set of simple APIs to handle asynchronous operations, taking care of timeout, retries, unnecessary try-catch blocks.
Now, here is how we achieve the equivalent result with `AsyncHandler`:

```java
AsyncHandler.withDefault().awaitWithTimeout(() -> "Success", 2_000L, "Default");
```
1 line of code, it computes the result asynchronously and gives you back the "Default" if it fails.
* Truly async, a task executed by a worker and then consumed asynchronously:
```java
ThrowableConsumer<String> resultHandler = result -> {
    System.out.println("Task completed successfully with result: " + result);
};
AsyncHandler.withDefault().awaitAsync(() -> "Hello World", resultHandler);
```
* Timeout with retries:
```java
AsyncHandler.withDefault().awaitWithTimeoutRetries(() -> "Success", 5_000L, 5, "Default");
```

* Retries with backoff strategy:
```java
AsyncHandler.withDefault().awaitBackoff(() -> someExpensiveOperation(), 5, "Default");
```
* Retries with backoff and timeout:
```java
AsyncHandler.withDefault().awaitBackoffWithTimeout(() -> someExpensiveOperation(), 60_000L, 5, "Default");
```
* `AsychHandler` can also be customized, with custom backoff configs and thread-pool size:

```java
// a backoff config with an initial delay of 100 milliseconds, a multiple of 2 and the max delay for 30 seconds
BackoffConfig backoffConfig = new BackoffConfig(100L, 2, 30_000);
AsyncHandler.withDefault().awaitBackoffWithBackoffConfig(() -> someExpensiveOperation(), 5, "Default", backoffConfig);

// custom pool size:
String val = AsyncHandler.withPoolSize(5).await(() -> "Result", "Default"); 
// with cached pool size:
String val = AsyncHandler.withCachedPoolSize(5).await(() -> "Result", "Default");
```

More detail documentation can be found in the `AsyncHandler` interface.
