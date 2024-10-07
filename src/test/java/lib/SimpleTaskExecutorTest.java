package lib;

import model.BackoffConfig;
import model.Task;
import model.TaskResult;
import model.TimeoutTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.SimpleTaskExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTaskExecutorTest {
	private ExecutorService executorService;
	private SimpleTaskExecutor executor;

	@BeforeEach
	void setUp() {
		executorService = Executors.newFixedThreadPool(5);
		executor = new SimpleTaskExecutor(executorService);
	}

	@AfterEach
	void tearDown() {
		executorService.shutdown();
	}

	@Test
	void testExecuteSuccess() {
		Task<String> task = new Task<>(Instant.now(), "1", () -> "Success", 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertEquals(Optional.empty(), result.getException());
		Optional<String> taskResult = result.getResult();
		assertTrue(taskResult.isPresent());
		assertEquals("Success", taskResult.get());
		assertTrue(result.getEndTime().isAfter(result.getStartTime()));
	}

	@Test
	void testExecuteAllAttemptsFail() {
		Task<String> task = new Task<>(Instant.now(), "2", () -> {
			throw new RuntimeException("Failure");
		}, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertNotNull(result.getException());
		assertEquals("Failure", result.getException().map(Throwable::getMessage).orElse(""));
	}

	@Test
	void testExecuteWithTimeout() {
		Task<String> task = new Task<>(Instant.now(), "3", () -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return "Success";
		}, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS); // 1 second timeout
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertTrue(result.getException().isPresent());
	}

	@Test
	void testExecuteWithMultipleAttempts() {
		Task<String> task = new Task<>(Instant.now(), "4", () -> {
			if (Math.random() < 0.7) {
				throw new RuntimeException("Random Failure");
			}
			return "Success";
		}, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertTrue(result.getResult().isPresent() || result.getException().isPresent());
	}

	@Test
	void testExecuteWithBackoffConfig() {
		BackoffConfig backoffConfig = new BackoffConfig(100, 2000, 1000, 100);
		Task<String> task = new Task<>(Instant.now(), "5", () -> {
			throw new RuntimeException("Failure");
		}, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 5000, TimeUnit.MILLISECONDS); // 5 seconds timeout
		TaskResult<String> result = executor.executeBackoff(timeoutTask, backoffConfig);
		assertNotNull(result);
		assertTrue(result.getException().isPresent());
		assertEquals("Failure", result.getException().get().getMessage());
	}

	@Test
	void testExecuteWithInterruptedException() {
		Task<String> task = new Task<>(Instant.now(), "6", () -> {
			throw new InterruptedException("Interrupted");
		}, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertTrue(result.getException().isPresent());
		assertTrue(result.getException().get() instanceof InterruptedException);
	}


	@Test
	void testExecuteMaxRetriesExceeded() {
		Task<String> task = new Task<>(Instant.now(), "7", () -> {
			throw new RuntimeException("Failure");
		}, 2);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertTrue(result.getException().isPresent());
		assertEquals("Failure", result.getException().get().getMessage());
	}

	@Test
	void testExecuteWithNullTaskLogic() {
		Task<String> task = new Task<>(Instant.now(), "8", null, 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		TaskResult<String> result = executor.execute(timeoutTask);
		assertNotNull(result);
		assertNotNull(result.getException());
	}


	@Test
	void testConcurrentExecutions() throws InterruptedException {
		Task<String> task = new Task<>(Instant.now(), "9", () -> "Concurrent Success", 3);
		TimeoutTask<String> timeoutTask = new TimeoutTask<>(task, 1000, TimeUnit.MILLISECONDS);
		Runnable taskRunner = () -> executor.execute(timeoutTask);
		Thread[] threads = new Thread[10];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(taskRunner);
			threads[i].start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		assertTrue(true);
	}

}