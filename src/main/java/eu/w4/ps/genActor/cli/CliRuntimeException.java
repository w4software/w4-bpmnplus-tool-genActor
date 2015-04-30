package eu.w4.ps.genActor.cli;

public class CliRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1440354028010724104L;

public CliRuntimeException() {
    super();
  }

  public CliRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  public CliRuntimeException(String message) {
    super(message);
  }

  public CliRuntimeException(Throwable cause) {
    super(cause);
  }

}
