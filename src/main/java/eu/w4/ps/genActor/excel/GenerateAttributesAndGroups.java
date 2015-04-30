package eu.w4.ps.genActor.excel;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import eu.w4.common.configuration.ConfigurationParameter;
import eu.w4.common.exception.CheckedException;
import eu.w4.engine.client.AttributeDefinition;
import eu.w4.engine.client.AttributeDefinitionFilter;
import eu.w4.engine.client.AttributeDefinitionIdentifier;
import eu.w4.engine.client.GroupIdentifier;
import eu.w4.engine.client.NetworkCommunicationException;
import eu.w4.engine.client.TypeDefinitionIdentifier;
import eu.w4.engine.client.UserIdentifier;
import eu.w4.engine.client.UserPropertyKey;
import eu.w4.engine.client.configuration.ConfigurationException;
import eu.w4.engine.client.configuration.NetworkConfigurationParameter;
import eu.w4.engine.client.service.AttributeDefinitionService;
import eu.w4.engine.client.service.AuthenticationService;
import eu.w4.engine.client.service.EngineService;
import eu.w4.engine.client.service.EngineServiceFactory;
import eu.w4.engine.client.service.GroupService;
import eu.w4.engine.client.service.ObjectFactory;
import eu.w4.engine.client.service.TypeDefinitionService;
import eu.w4.engine.client.service.UserService;

public class GenerateAttributesAndGroups {

	private static AttributeDefinitionIdentifier createAttribute(
			Principal principal, ObjectFactory factory,
			AttributeDefinitionService attributeService, String prefix,
			String name, String type) throws RemoteException, CheckedException {

		TypeDefinitionIdentifier typeDefinitionIdentifier = factory
				.newTypeDefinitionIdentifier();

		if (type.toUpperCase().equals("STRING")) {
			typeDefinitionIdentifier
					.setId(TypeDefinitionService.STRING_TYPE_DEFINITION_ID);
		} else if (type.toUpperCase().equals("LIST_STRING")) {
			typeDefinitionIdentifier
					.setId(TypeDefinitionService.STRING_LIST_TYPE_DEFINITION_ID);
		}

		return attributeService.createAttributeDefinition(principal, null,
				prefix, name, null, typeDefinitionIdentifier, null);
	}


	private static AttributeDefinitionIdentifier getCreateAttributeDefinition(
			Principal principal, ObjectFactory factory,
			AttributeDefinitionService attributeService, String prefix,
			String name, String type) throws RemoteException, CheckedException {
		AttributeDefinitionFilter attributeDefinitionFilter = factory
				.newAttributeDefinitionFilter();
		attributeDefinitionFilter.attributeDefinitionNameLike(name);
		attributeDefinitionFilter.attributeDefinitionPrefixLike(prefix);

		List<AttributeDefinition> attributeDefinitions = attributeService
				.searchAttributeDefinitions(principal, null,
						attributeDefinitionFilter, null, null, null);
		if (attributeDefinitions != null && attributeDefinitions.size() > 0) {
			return attributeDefinitions.get(0).getIdentifier();
		} else {
			return createAttribute(principal, factory, attributeService,
					prefix, name, type);
		}
	}


	private final Principal _principal;
	private final EngineService _engineService;

	public GenerateAttributesAndGroups(Principal principal, EngineService engineService) {

		this._principal = principal;
		this._engineService = engineService;
	}

	public GenerateAttributesAndGroups(final String serverName,
			final String portNumber, final String login, final String password) throws NetworkCommunicationException, ConfigurationException, CheckedException, RemoteException {
		Map<ConfigurationParameter, String> conf = new HashMap<ConfigurationParameter, String>();
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_HOST, serverName);
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_PORT, portNumber);

		this._engineService = EngineServiceFactory
				.getEngineService(conf);
		AuthenticationService authService = _engineService
				.getAuthenticationService();
		this._principal = authService.login(login, password);
	}

	public void createAttributesAndGroups(File file, int sheetNumber) throws CheckedException, BiffException, IOException {

		ObjectFactory factory = _engineService.getObjectFactory();
		UserService userService = _engineService.getUserService();
		GroupService groupService = _engineService.getGroupService();
		AttributeDefinitionService attributeService = _engineService
				.getAttributeDefinitionService();

		final Workbook xlsWorkbook = Workbook.getWorkbook(file);
		final Sheet xlsSheet = xlsWorkbook.getSheet(sheetNumber);

		final int rows = xlsSheet.getRows();

		for (int r = 1; r < rows; r++) {

			if (xlsSheet.getCell(0, r).getContents().equals("ATTRIBUT")) {

				System.out.println("\nCreation of an attribute");
				try {
					createAttribute(_principal, factory, attributeService,
							xlsSheet.getCell(1, r).getContents(), xlsSheet
							.getCell(2, r).getContents(), xlsSheet
							.getCell(4, r).getContents());
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}

			}
			if (xlsSheet.getCell(0, r).getContents().equals("USER")) {
				System.out
						.println("\nCreation of a user --> Prefer the other creation option");

				try {
					HashMap<String, Object> properties = new HashMap<String, Object>();
					properties.put(UserPropertyKey.FIRST_NAME, xlsSheet
							.getCell(2, r).getContents());
					properties.put(UserPropertyKey.LAST_NAME,
							xlsSheet.getCell(1, r).getContents());
					properties.put(UserPropertyKey.EMAIL, xlsSheet
							.getCell(5, r).getContents());

					UserIdentifier myUser = userService.createUser(
							_principal, null, xlsSheet.getCell(3, r)
									.getContents(), // login
							xlsSheet.getCell(4, r).getContents(), // mot de
							// passe
							null, properties, true);

					String attribute = xlsSheet.getCell(6, r).getContents();
					if (attribute != null) {
						String[] values = attribute.split(":");
						userService.addUserAttribute(
								_principal,
								myUser,
								getCreateAttributeDefinition(_principal,
										factory, attributeService, values[0],
										values[1], null), values[2]);
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}

			if (xlsSheet.getCell(0, r).getContents().equals("GROUP")) {
				System.out.println("\nCreation of a group");

				try {
					GroupIdentifier myGroup = null;
					if (xlsSheet.getCell(2, r).getContents().equals("")) {
						myGroup = groupService.createGroup(_principal, null,
								xlsSheet.getCell(1, r).getContents(), null);
					} else {
						GroupIdentifier groupParentId = factory
								.newGroupIdentifier();
						groupParentId.setName(xlsSheet.getCell(2, r)
								.getContents());
						myGroup = groupService.createGroup(_principal,
								groupParentId, xlsSheet.getCell(1, r)
										.getContents(), null);
					}

					int i = 3;
					while (xlsSheet.getCell(i, r).getContents() != null
							&& !xlsSheet.getCell(i, r).getContents().equals("")) {
						String attribute = xlsSheet.getCell(i, r).getContents();
						String[] values = attribute.split(":");
						if (values[2].startsWith("[")) {
							String listValues = values[2].substring(1,
									values[2].length() - 1);

							List<String> list = new ArrayList<String>();
							String[] tab = listValues.split(";");
							for (String element : tab) {
								list.add(element);
							}
							AttributeDefinitionIdentifier attributeDefinition = getCreateAttributeDefinition(
									_principal, factory, attributeService,
									values[0], values[1], null);
							groupService.addGroupAttribute(_principal,
									myGroup, attributeDefinition, list);
						} else {
							groupService.addGroupAttribute(
									_principal,
									myGroup,
									getCreateAttributeDefinition(_principal,
											factory, attributeService,
											values[0], values[1], null),
											values[2]);
						}
						i++;
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			if (xlsSheet.getCell(0, r).getContents().equals("ASSIGNATION")) {
				System.out.println("\nGroup affectation to : "
						+ xlsSheet.getCell(1, r).getContents());

				try {
					UserIdentifier user = factory.newUserIdentifier();
					user.setName(xlsSheet.getCell(1, r).getContents());

					String listGroup = xlsSheet.getCell(2, r).getContents();
					listGroup = listGroup.substring(1, listGroup.length() - 1);
					if (!listGroup.equals("")) {
						String[] values = listGroup.split(";");
						for (String value : values) {
							GroupIdentifier group = factory
									.newGroupIdentifier();
							group.setName(value);
							userService.addUserToGroup(_principal, user,
									group);
						}
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
		System.out.println("done.");
	}	
}
