package exception;

import util.ParsingUtils;

import java.io.Serial;

public class SimpleAsyncException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = -7034897190745766939L;

  public SimpleAsyncException(String message) {
    super(message);
  }

  public SimpleAsyncException(Throwable throwable) {
    super(throwable);
  }

  public SimpleAsyncException(String message, Throwable cause) {
    super(message, cause);
  }

  public static SimpleAsyncException getException(Throwable cause, String message, Object...params) {
    String errorMessage = ParsingUtils.getLoggedMessage(message, params);
    return new SimpleAsyncException(errorMessage, cause);
  }


  public static SimpleAsyncException getException(String message, Object...params) {
    String errorMessage = ParsingUtils.getLoggedMessage(message, params);
    return new SimpleAsyncException(errorMessage);
  }

}
