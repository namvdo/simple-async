package pub.fi;

@FunctionalInterface
public interface ThrowableSupplier<T> {
  T get() throws Exception;
}
