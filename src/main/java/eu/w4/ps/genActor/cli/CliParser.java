package eu.w4.ps.genActor.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command Line Interface
 */
public class CliParser {


  


  private final static Pattern PATTERN_OPTION;
  private final static Pattern PATTERN_COMMANDLINE;
  static {
    String rxParameter = "\"([^\"]*)\"|([^ -]*)";
    String rxOption = "-(\\w*) *(" + rxParameter + ")";
    rxOption = rxOption.replaceAll("\"", "(?<!\\\\\\\\)\"");
    PATTERN_OPTION = Pattern.compile(rxOption);
    String rxCommandLine = "(" + rxOption + " *)*";
    PATTERN_COMMANDLINE = Pattern.compile(rxCommandLine);
  }


  private Collection<CliParameter> parameters;


  public CliParser() {
    parameters = new ArrayList<CliParameter>();
  }


  public void addParameter(final CliParameter parameter) {
    this.parameters.add(parameter);
  }


  private Properties parseLine(String str) throws CliException {
    if (!PATTERN_COMMANDLINE.matcher(str).matches()) {
      throw new RuntimeException("Invalid command line");
    }
    final Properties result = new Properties();
    final Matcher cliMatcher = PATTERN_OPTION.matcher(str);
    cliMatcher.reset();
    while (cliMatcher.find()) {
      final String name = cliMatcher.group(1);
      final String value = "" + (cliMatcher.group(3) == null ? cliMatcher.group(4) : cliMatcher.group(3));
      result.put(name, value);
    }
    return result;
  }


  public Properties parse(String str) throws CliException {
    final Properties result = parseLine(str);
    for (CliParameter p : parameters) {
      if (!result.containsKey(p.getName())) {
        if (p.isRequired()) {
          throw new CliException("The parameter -" + p.getName() + " is required");
        } else {
          if (p.getDefaultValue() != null && !p.isFlag()) {
            result.put(p.getName(), p.getDefaultValue());
          }
        }
      }
    }
    return result;
  }

  public Properties parse(String[] str) throws CliException {
    final StringBuilder builder = new StringBuilder();
    for (String s : str)
      builder.append(s.indexOf(' ') != -1 && !s.startsWith("\"") ? "\"" + s + "\"" : s).append(" ");
    return this.parse(builder.toString());
  }

  public String getShortHelp() {
    final StringBuilder builder = new StringBuilder();
    for (final CliParameter p : parameters) {
      builder.append("\t").append(p.getName()).append("\t").append(p.getDescription()).append("\n");
    }
    return builder.toString();
  }
}
