package pub;

import exception.SimpleAsyncException;
import model.BackoffConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pub.fi.ThrowableConsumer;
import pub.fi.ThrowableSupplier;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncHandlerTest {

	private AsyncHandler asyncHandler;

	@BeforeEach
	void setUp() {
		asyncHandler = AsyncHandler.withDefault();
	}

	@Test
	void testAwaitNullableWithTimeoutRetriesSuccess() {
		ThrowableSupplier<String> supplier = () -> "Success";
		String result = asyncHandler.await(supplier, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitNullableWithTimeoutRetriesFailureReturnsDefault() {
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Failure");
		};
		String result = asyncHandler.await(supplier, "Default");
		assertEquals("Default", result);
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeout() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(2000); // Simulate a long task
			return "Success";
		};
		String result = asyncHandler.awaitWithTimeout(supplier, 1000L, "Timeout");
		assertEquals("Timeout", result);
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndRetryCount() {
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		String result = asyncHandler.awaitWithRetries(supplier, 3, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeoutAndRetryCount() {
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		String result = asyncHandler.awaitWithTimeoutRetries(supplier, 1000L, 3, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitNullableWithTimeoutRetriesOptional() {
		ThrowableSupplier<String> supplier = () -> "Success";
		Optional<String> result = asyncHandler.awaitNullable(supplier);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitNullableWithRetriesOptionalTimeoutAndTimeout() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(2000);
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeout(supplier, 1000L);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitNullableWithRetriesOptionalTimeoutAndRetryCount() {
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		Optional<String> result = asyncHandler.awaitNullableWithRetries(supplier, 3);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitBackoffSuccess() {
		ThrowableSupplier<String> supplier = () -> "Success";
		String result = asyncHandler.awaitBackoff(supplier, 3, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitBackoffFailureReturnsDefault() {
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Failure");
		};
		String result = asyncHandler.awaitBackoff(supplier, 3, "Default");
		assertEquals("Default", result);
	}

	@Test
	void testAwaitBackoffWithTimeoutRetriesRetryCount() {
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		String result = asyncHandler.awaitBackoff(supplier, 3, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitBackoffTimeoutRetriesWithTimeoutAndRetryCount() {
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		String result = asyncHandler.awaitBackoffWithTimeout(supplier, 20_000L, 3, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testAwaitBackoffOptional() {
		ThrowableSupplier<String> supplier = () -> "Success";
		Optional<String> result = asyncHandler.awaitNullableBackoff(supplier, 3);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitBackoffTimeoutRetriesOptionalWithTimeout() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(2000); // Simulate long task
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableBackoffWithTimeout(supplier, 1000L, 3);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitBackoffBackoffTimeoutRetriesConfig() {
		BackoffConfig backoffConfig = new BackoffConfig(100, 2, 1000, 100);
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Failure");
		};
		String result = asyncHandler.awaitBackoffWithBackoffConfig(supplier, 3, "Default", backoffConfig);
		assertEquals("Default", result);
	}

	@Test
	void testAwaitBackoffOptionalBackoffTimeoutRetriesConfig() {
		BackoffConfig backoffConfig = new BackoffConfig(100, 2, 1000, 100);
		ThrowableSupplier<String> supplier = new ThrowableSupplier<>() {
            private int attempts = 0;

            @Override
            public String get() {
                if (attempts++ < 2) {
                    throw new RuntimeException("Failure");
                }
                return "Success";
            }
        };
		Optional<String> result = asyncHandler.awaitNullableBackoffWithTimeoutBackoffConfig(supplier, 10_000L, 3, backoffConfig);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testWithCustomThreadPool() {
		AsyncHandler customHandler = AsyncHandler.withPoolSize(10);
		ThrowableSupplier<String> supplier = () -> "Success";
		String result = customHandler.await(supplier, "Default");
		assertEquals("Success", result);
	}

	@Test
	void testWithCachedPoolSize() {
		AsyncHandler customHandler = AsyncHandler.withCachedPoolSize(10);
		ThrowableSupplier<String> supplier = () -> "Success";
		String result = customHandler.await(supplier, "Default");
		assertEquals("Success", result);
	}


	@Test
	void testAwaitNullableWithRetriesOptionalTimeoutAndTimeout_Success() {
		ThrowableSupplier<String> supplier = () -> "Success";
		Optional<String> result = asyncHandler.awaitNullableWithRetries(supplier, 1000); // Timeout of 1 second
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}


	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeoutAndRetry_SuccessOnFirstAttempt() {
		ThrowableSupplier<String> supplier = () -> "Success";
		Optional<String> result = asyncHandler.awaitNullableWithTimeoutRetries(supplier, 1000L, 3);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeoutAndRetry_SuccessOnRetry() {
		AtomicInteger attemptCount = new AtomicInteger(0);
		ThrowableSupplier<String> supplier = () -> {
			if (attemptCount.incrementAndGet() < 2) {
				throw new RuntimeException("Failed attempt");
			}
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeoutRetries(supplier, 1000L, 3);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
		assertEquals(2, attemptCount.get());
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndRetry_FailureAfterTimeoutAndRetries() {
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Failed attempt");
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeoutRetries(supplier, 1000L, 3);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeoutAndRetry_TimeoutBeforeSuccess() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(2000);
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeoutRetries(supplier, 500L, 3);
		assertFalse(result.isPresent());
	}



	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeout_Failure() {
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Task failed");
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeout(supplier, 1000L);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeout_TaskExceedsTimeout() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(1500);
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeout(supplier, 1000L);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitNullableWithRetriesTimeoutAndTimeout_TaskCompletesWithinTimeout() {
		ThrowableSupplier<String> supplier = () -> {
			Thread.sleep(500);
			return "Success";
		};
		Optional<String> result = asyncHandler.awaitNullableWithTimeout(supplier, 1000L);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitWithTimeout_NullTaskNullableNullableWithRetriesByRetries() {
		ThrowableSupplier<String> supplier = () -> null;
		Optional<String> result = asyncHandler.awaitNullableWithTimeout(supplier, 1000L);
		assertFalse(result.isPresent());
	}


	@Test
	void testAwaitBackoff_SuccessFirstAttempt() {
		ThrowableSupplier<String> supplier = () -> "Success";
		BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
	}

	@Test
	void testAwaitBackoff_SuccessAfterRetries() {
		AtomicInteger attempt = new AtomicInteger(0);
		ThrowableSupplier<String> supplier = () -> {
			if (attempt.incrementAndGet() < 3) {
				throw new RuntimeException("Temporary failure");
			}
			return "Success";
		};
		BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
		assertEquals(3, attempt.get());
	}

	@Test
	void testAwaitBackoff_FailureAfterMaxRetries() {
		ThrowableSupplier<String> supplier = () -> {
			throw new RuntimeException("Task failed");
		};
		BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		assertFalse(result.isPresent());
	}

	@Test
	void testAwaitBackoff_SuccessOnLastRetry() {
		AtomicInteger attempt = new AtomicInteger(0);
		ThrowableSupplier<String> supplier = () -> {
			if (attempt.incrementAndGet() < 3) {
				throw new RuntimeException("Temporary failure");
			}
			return "Success";
		};
		BackoffConfig backoffConfig = new BackoffConfig(200, 2.0, 1000, 50); // 200ms initial delay, exponential backoff
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
		assertEquals(3, attempt.get());
	}

	@Test
	void testAwaitBackoff_BackoffDelaysApplied() {
		AtomicInteger attempt = new AtomicInteger(0);
		long[] delays = new long[3];
		ThrowableSupplier<String> supplier = () -> {
			if (attempt.incrementAndGet() < 3) {
				delays[attempt.get() - 1] = System.currentTimeMillis();
				throw new RuntimeException("Temporary failure");
			}
			return "Success";
		};

		BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 500, 50); // Initial delay of 100ms, doubling
		long startTime = System.currentTimeMillis();
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		long totalTime = System.currentTimeMillis() - startTime;
		assertTrue(result.isPresent());
		assertEquals("Success", result.get());
		assertTrue(delays[0] - startTime >= 100, "First retry delay should be at least 100ms");
		assertTrue(delays[1] - delays[0] >= 200, "Second retry delay should be at least 200ms (2x backoff)");
		assertTrue(totalTime >= 300, "Total time should account for backoff delays");
	}

	@Test
	void testAwaitBackoff_NullOnFailure() {
		ThrowableSupplier<String> supplier = () -> null;
		BackoffConfig backoffConfig = new BackoffConfig(100, 2.0, 1000, 50);
		Optional<String> result = asyncHandler.awaitNullableBackoffWithBackoffConfig(supplier, 3, backoffConfig);
		assertFalse(result.isPresent());
	}


	@Test
	void testAwaitAsync_successfulExecution() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean callbackInvoked = new AtomicBoolean(false);
		ThrowableSupplier<String> task = () -> "Success";
		ThrowableConsumer<String> resultHandler = result -> {
			assertEquals("Success", result);
			callbackInvoked.set(true);
			latch.countDown();
		};
		asyncHandler.awaitAsync(task, resultHandler);
		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertTrue(callbackInvoked.get());
	}

	@Test
	void testAwaitAsync_successfulExecutionWithSleep() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean callbackInvoked = new AtomicBoolean(false);
		ThrowableSupplier<String> task = () -> "Success";
		ThrowableConsumer<String> resultHandler = result -> {
			assertEquals("Success", result);
			Thread.sleep(1000L);
			callbackInvoked.set(true);
			latch.countDown();
		};
		asyncHandler.awaitAsync(task, resultHandler);
		assertTrue(latch.await(3, TimeUnit.SECONDS));
		assertTrue(callbackInvoked.get());
	}

	@Test
	void testAwaitAsync_notBlockToGetAsyncResult() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicBoolean resultHandlerResult = new AtomicBoolean(false);
		ThrowableSupplier<String> supplier = () -> "Success";
		ThrowableConsumer<String> consumer = (str) -> {
			Thread.sleep(2_000);
			resultHandlerResult.set(true);
			countDownLatch.countDown();
		};
		asyncHandler.awaitAsync(supplier, consumer);
		assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
		assertFalse(resultHandlerResult.get());
	}

	@Test
	void testAwaitAsync_nullTask() {
		assertThrows(SimpleAsyncException.class, () -> {
			asyncHandler.awaitAsync(null, result -> {
			});
		});
	}

	@Test
	void testAwaitAsync_nullResultHandler() {
		assertThrows(SimpleAsyncException.class, () -> {
			asyncHandler.awaitAsync(() -> "Success", null);
		});
	}

}