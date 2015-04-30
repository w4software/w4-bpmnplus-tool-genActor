package eu.w4.ps.genActor.cli;

public class CliException extends Exception {

  
	private static final long serialVersionUID = -5908347137095297293L;

public CliException() {
    super();
  }

  public CliException(String message, Throwable cause) {
    super(message, cause);
  }

  public CliException(String message) {
    super(message);
  }

  public CliException(Throwable cause) {
    super(cause);
  }

}
