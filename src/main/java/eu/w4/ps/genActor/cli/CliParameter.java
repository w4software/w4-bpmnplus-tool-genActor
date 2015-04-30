package eu.w4.ps.genActor.cli;

/**
 * Command Line Interface Option
 */
public class CliParameter {

  
  private final String name;
  private final String description;
  private final String defaultValue;
//with or whithout value
  private final boolean flag;
  private final boolean required;


  public CliParameter(final String name, final String description, final String defaultValue, final boolean flag, final boolean required) {
    super();
    this.name = name;
    this.description = description;
    this.defaultValue = defaultValue;
    this.flag = flag;
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isFlag() {
    return flag;
  }

}
