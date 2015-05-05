package eu.w4.ps.genActor.excel;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import eu.w4.common.configuration.ConfigurationParameter;
import eu.w4.common.exception.CheckedException;
import eu.w4.engine.client.AttributeDefinition;
import eu.w4.engine.client.AttributeDefinitionFilter;
import eu.w4.engine.client.AttributeDefinitionIdentifier;
import eu.w4.engine.client.NetworkCommunicationException;
import eu.w4.engine.client.TypeDefinitionIdentifier;
import eu.w4.engine.client.UserAlreadyExistsException;
import eu.w4.engine.client.UserIdentifier;
import eu.w4.engine.client.UserPropertyKey;
import eu.w4.engine.client.configuration.ConfigurationException;
import eu.w4.engine.client.configuration.NetworkConfigurationParameter;
import eu.w4.engine.client.mail.EmailNotification;
import eu.w4.engine.client.service.AttributeDefinitionService;
import eu.w4.engine.client.service.AuthenticationService;
import eu.w4.engine.client.service.EngineService;
import eu.w4.engine.client.service.EngineServiceFactory;
import eu.w4.engine.client.service.ObjectFactory;
import eu.w4.engine.client.service.TypeDefinitionService;
import eu.w4.engine.client.service.UserService;
import eu.w4.ps.genActor.cli.CliPrinter;

public class GenerateActors {

	private static AttributeDefinitionIdentifier getCreateAttributeDefinition(
			Principal principal, ObjectFactory factory,
			AttributeDefinitionService attributeService, String prefixe,
			String nom, String type) throws RemoteException, CheckedException {
		AttributeDefinitionFilter attributeDefinitionFilter = factory
				.newAttributeDefinitionFilter();
		attributeDefinitionFilter.attributeDefinitionNameLike(nom);
		attributeDefinitionFilter.attributeDefinitionPrefixLike(prefixe);

		List<AttributeDefinition> attributeDefinitions = attributeService
				.searchAttributeDefinitions(principal, null,
						attributeDefinitionFilter, null, null, null);
		if (attributeDefinitions != null && attributeDefinitions.size() > 0) {
			return attributeDefinitions.get(0).getIdentifier();
		}
		TypeDefinitionIdentifier typeDefinitionIdentifier = factory
				.newTypeDefinitionIdentifier();

		if (type == "DATE") {
			typeDefinitionIdentifier
			.setId(TypeDefinitionService.DATE_TYPE_DEFINITION_ID);
		} else if (type == "STRING") {
			typeDefinitionIdentifier
			.setId(TypeDefinitionService.STRING_TYPE_DEFINITION_ID);
		}

		return attributeService.createAttributeDefinition(principal, null,
				prefixe, nom, null, typeDefinitionIdentifier, null);
	}

	private final Principal _principal;
	private final EngineService _engineService;

	public GenerateActors(Principal principal, EngineService engineService) {
		super();

		this._principal = principal;
		this._engineService = engineService;
	}

	public GenerateActors(final String serverName, final String portNumber,
			final String login, final String password) throws NetworkCommunicationException, ConfigurationException, CheckedException, RemoteException {
		super();

		Map<ConfigurationParameter, String> conf = new HashMap<ConfigurationParameter, String>();
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_HOST, serverName);
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_PORT, portNumber);

		this._engineService = EngineServiceFactory
				.getEngineService(conf);
		AuthenticationService authService = _engineService
				.getAuthenticationService();
		this._principal = authService.login(login, password);
	}

	public void createUsers(File file, int sheetNumber) throws BiffException, IOException, CheckedException {
		List<Object> lignes = read(file, sheetNumber);

		for (Object currentUser : lignes) {
			if (currentUser instanceof UserBPMN) {
				UserService userService = _engineService.getUserService();
				Map<String,Object> userProperties = ((UserBPMN) currentUser).getProperties();
				userProperties.put(UserPropertyKey.EMAIL_NOTIFICATION, EmailNotification.INSTANTANEOUSLY);
				try {
					UserIdentifier myUser = userService.createUser(
							_principal, null,
							((UserBPMN) currentUser).getLogin(),
							((UserBPMN) currentUser).getPassword(), null,
							userProperties, true);

					ObjectFactory factory = _engineService.getObjectFactory();
					AttributeDefinitionService attributeService = _engineService
							.getAttributeDefinitionService();

					for (Entry<String, String> entry : ((UserBPMN) currentUser)
							.getAttributes().entrySet()) {
						String[] cle = entry.getKey().split(":");

						if (cle.length != 2) {
							CliPrinter.println("ERROR in the attributes");
							break;
						}
						try {
							AttributeDefinitionIdentifier attribut = getCreateAttributeDefinition(
									_principal, factory, attributeService,
									cle[0], cle[1], "STRING");
							String valeur = entry.getValue();
							userService.addUserAttribute(_principal, myUser,
									attribut, valeur);

						} catch (UserAlreadyExistsException e) {
							CliPrinter.println("The user : "
									+ ((UserBPMN) currentUser).getLogin()
									+ " already exists. It was not created");
						} catch (CheckedException e) {
							CliPrinter
							.println("ERROR : the attribute doesn't exists or its type isn't String");
						}
					}
				} catch (UserAlreadyExistsException e) {
					CliPrinter.println("The user : "
							+ ((UserBPMN) currentUser).getLogin()
							+ " already exists. It was not created");
				}

			} else {
				CliPrinter.print("not supported");
			}
		}

	}


	private List<Object> read(File file, int sheetNumber) throws BiffException,
	IOException {
		// loading of the workbook
		final Workbook xlsWorkbook = Workbook.getWorkbook(file);
		final Sheet xlsSheet = xlsWorkbook.getSheet(sheetNumber);
		final int rows = xlsSheet.getRows();
		final int columns = xlsSheet.getColumns();
		final String[] headers;

		if (rows < 2) {
			throw new RuntimeException(
					"Sheet must contains at least two lines (header and one content line");
		}

		Map<String, Integer> mapColumns = new HashMap<String, Integer>();
		List<Object> myActors = new ArrayList<Object>();

		headers = new String[columns];
		for (int c = 0; c < columns; c++) {
			String header = xlsSheet.getCell(c, 0).getContents();
			if (header == null || "".equals(header)) {
				headers[c] = null;
			} else {
				headers[c] = header;
				mapColumns.put(header, c);
			}
		}

		// lecture of the other ligns and construction of the result
		lrows: for (int r = 1; r < rows; r++) {

			final UserBPMN object;
			if ("TYPE".equalsIgnoreCase(headers[0])) {
				String typeContent = xlsSheet.getCell(0, r).getContents();
				if ("ACTOR".equalsIgnoreCase(typeContent)) {
					object = new UserBPMN();
					for (int c = 1; c < columns; c++) {
						if (headers[c] == null) {
							continue;
						}
						String cellContent = xlsSheet.getCell(c, r)
								.getContents();
						if (cellContent != null && !"".equals(cellContent)) {

							if ("LOGIN".equalsIgnoreCase(headers[c])) {
								object.setLogin(cellContent);
							}
							if ("PASSWORD".equalsIgnoreCase(headers[c])) {
								object.setPassword(cellContent);
							}
							if (headers[c].startsWith("property")) {
								String propertyName = headers[c].substring(9,
										headers[c].length());
								object.setProperties(propertyName, cellContent);
							}
							if (headers[c].startsWith("attribute")) {
								String attributeName = headers[c].substring(10,
										headers[c].length());
								object.setAttributes(attributeName, cellContent);
							}
						}
					}
					myActors.add(object);

				} else {
					System.err.println("ERROR: Unknown object type <"
							+ typeContent + "> on line " + r);
					continue lrows;
				}
			}

		}

		CliPrinter.println("" + (rows - 1) + " actors read from Excel file");
		return myActors;
	}
}
