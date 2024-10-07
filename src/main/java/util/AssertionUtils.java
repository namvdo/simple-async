package util;

import exception.SimpleAsyncException;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

public class AssertionUtils {

	public static boolean notNull(Object object) {
		return Objects.nonNull(object);
	}

	public static void nonNullOrThrow(Object object, String message)  {
		if (!notNull(object)) {
			throw new SimpleAsyncException(message);
		}
	}

	public static void nonNullOrThrow(Object object)  {
		nonNullOrThrow(object, "The passed parameter must not be null.");
	}

	public static void isPositiveInteger(Number num) {
		if (num == null || num.intValue() <= 0) {
			throw SimpleAsyncException.getException("Passed parameter must be positive, received: {}", num);
		}
	}

	public static boolean allNonNull(Object...objects) {
		if (objects == null) throw new SimpleAsyncException("Passed objects cannot be null");
		for(final Object object: objects) {
			if (!notNull(object)) {
				return false;
			}
		}
		return true;
	}

	public <T> boolean allValid(Collection<T> collection, Predicate<T> predicate) {
		if (CollectionUtils.isEmpty(collection)) throw new SimpleAsyncException("Collection is empty");
		for(final T item : collection) {
			if (!predicate.test(item)) return false;
		}
		return true;
	}

	public <T> boolean allInvalid(Collection<T> collection, Predicate<T> predicate) {
		if (CollectionUtils.isEmpty(collection)) throw new SimpleAsyncException("Collection is empty.");
		for(final T item : collection) {
			if (predicate.test(item)) return false;
		}
		return true;
	}
}
