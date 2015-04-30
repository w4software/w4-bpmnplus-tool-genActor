package eu.w4.ps.genActor.cli;

public final class CliPrinter {

  private CliPrinter() {
    // prevent instantiation
  }


  private static boolean verbosity;

  public static void setVerbose(boolean verbosity) {
    CliPrinter.verbosity = verbosity;
  }

  public static void print(final String message) {
    System.out.print(message);
  }

  public static void println(final String message) {
    System.out.println(message);
  }

  public static void verbose(final String message) {
    if (verbosity)
      System.out.print(message);
  }

  public static void verboseln(final String message) {
    if (verbosity)
      System.out.println(message);
  }

}
