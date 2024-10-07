package pub.fi;


import lombok.extern.log4j.Log4j2;
import model.TaskResult;

public interface ThrowableConsumer<T> {
	void accept(T value) throws Exception;

	default void onError(TaskResult<T> errorTaskResult) {
		TaskResultErrorMessage.logErrorMessage(errorTaskResult);
	}

	@Log4j2
	final class TaskResultErrorMessage {
		public static <E> void logErrorMessage(TaskResult<E> errorTaskResult) {
			log.error("Exception occurred while processing async task: {}," +
					" thread name: {}, " +
					"start: {}, " +
					"end: {}, " +
					"retry count: {}, " +
					"exception: {}, {}",
					errorTaskResult.getTaskId(),
					errorTaskResult.getThreadName(),
					errorTaskResult.getStartTime(),
					errorTaskResult.getEndTime(),
					errorTaskResult.getRetryCount().get(),
					errorTaskResult.getException()
							.orElse(null));
		}
	}
}
