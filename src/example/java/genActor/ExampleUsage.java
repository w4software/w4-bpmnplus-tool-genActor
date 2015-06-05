package genActor;

import java.io.File;

import eu.w4.ps.genActor.excel.GenerateActors;

public class ExampleUsage
{

  public static void main(String... args)
    throws Exception
  {
    GenerateActors generateActors = new GenerateActors("localhost",
                                                       "7707",
                                                       "admin",
                                                       "admin");

    File file = new File("/path/to/ExcelFile.xlsx");

    generateActors.createUsers(file, 0);
  }
}
