package eu.w4.ps.genActor.excel;

import java.io.File;
import java.util.Properties;

import eu.w4.ps.genActor.cli.CliException;
import eu.w4.ps.genActor.cli.CliParameter;
import eu.w4.ps.genActor.cli.CliParser;
import eu.w4.ps.genActor.cli.CliPrinter;
import eu.w4.ps.genActor.excel.GenerateActors;
import eu.w4.ps.genActor.excel.GenerateAttributesAndGroups;

public class Main {

	public static void main(String[] args) {
		try {
			final Properties props;
			final CliParser parser = new CliParser();

			parser.addParameter(new CliParameter("m", "method", "1", false,
					false));
			parser.addParameter(new CliParameter("s", "server name", "",
					false, false));
			parser.addParameter(new CliParameter("p", "RMI port", "7707",
					false, false));
			parser.addParameter(new CliParameter("l", "login", "admin", false,
					false));
			parser.addParameter(new CliParameter("w", "password", "admin",
					false, false));
			parser.addParameter(new CliParameter("f", "XLS file", null,
					false, true));

			try {
				props = parser.parse(args);
			} catch (CliException e) {
				CliPrinter.println("Error: " + e.getMessage());
				CliPrinter.println(parser.getShortHelp());
				System.exit(1);
				return;
			}

			File xlsFile = new File(props.getProperty("f"));
			String srv = props.getProperty("s");
			String portNumber = props.getProperty("p");
			String login = props.getProperty("l");
			String passwd = props.getProperty("w");

			try {
				switch (Integer.parseInt(props.getProperty("m"))) {

				case 1:
					GenerateActors ga = new GenerateActors(srv, portNumber,
							login, passwd);
					ga.createUsers(xlsFile, 0);
					break;
				case 2:
					GenerateAttributesAndGroups attGrp = new GenerateAttributesAndGroups(
							srv, portNumber, login, passwd);
					attGrp.createAttributesAndGroups(xlsFile, 0);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				CliPrinter.println("Error: " + e.getMessage());
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}

}
