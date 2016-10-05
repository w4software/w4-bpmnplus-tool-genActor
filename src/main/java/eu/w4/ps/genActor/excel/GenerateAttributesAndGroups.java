package eu.w4.ps.genActor.excel;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.w4.common.configuration.ConfigurationParameter;
import eu.w4.common.exception.CheckedException;
import eu.w4.engine.client.AttributeDefinition;
import eu.w4.engine.client.AttributeDefinitionFilter;
import eu.w4.engine.client.AttributeDefinitionIdentifier;
import eu.w4.engine.client.GroupIdentifier;
import eu.w4.engine.client.LanguageIdentifier;
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
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.read.biff.BiffException;

public class GenerateAttributesAndGroups {

	private static AttributeDefinitionIdentifier createAttribute(Principal principal, ObjectFactory factory,
			AttributeDefinitionService attributeService, String prefix, String name, String type)
			throws RemoteException, CheckedException {

		TypeDefinitionIdentifier typeDefinitionIdentifier = factory.newTypeDefinitionIdentifier();

		if (type.toUpperCase().equals("STRING")) {
			typeDefinitionIdentifier.setId(TypeDefinitionService.STRING_TYPE_DEFINITION_ID);
		} else if (type.toUpperCase().equals("LIST_STRING")) {
			typeDefinitionIdentifier.setId(TypeDefinitionService.STRING_LIST_TYPE_DEFINITION_ID);
		}

		return attributeService.createAttributeDefinition(principal, null, prefix, name, null, typeDefinitionIdentifier,
				null);
	}

	private static AttributeDefinitionIdentifier getCreateAttributeDefinition(Principal principal,
			ObjectFactory factory, AttributeDefinitionService attributeService, String prefix, String name, String type)
			throws RemoteException, CheckedException {
		AttributeDefinitionFilter attributeDefinitionFilter = factory.newAttributeDefinitionFilter();
		attributeDefinitionFilter.attributeDefinitionNameLike(name);
		attributeDefinitionFilter.attributeDefinitionPrefixLike(prefix);

		List<AttributeDefinition> attributeDefinitions = attributeService.searchAttributeDefinitions(principal, null,
				attributeDefinitionFilter, null, null, null);
		if (attributeDefinitions != null && attributeDefinitions.size() > 0) {
			return attributeDefinitions.get(0).getIdentifier();
		} else {
			return createAttribute(principal, factory, attributeService, prefix, name, type);
		}
	}

	private final Principal _principal;
	private final EngineService _engineService;

	public GenerateAttributesAndGroups(Principal principal, EngineService engineService) {

		this._principal = principal;
		this._engineService = engineService;
	}

	public GenerateAttributesAndGroups(final String serverName, final String portNumber, final String login,
			final String password)
			throws NetworkCommunicationException, ConfigurationException, CheckedException, RemoteException {
		Map<ConfigurationParameter, String> conf = new HashMap<ConfigurationParameter, String>();
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_HOST, serverName);
		conf.put(NetworkConfigurationParameter.RMI__REGISTRY_PORT, portNumber);

		this._engineService = EngineServiceFactory.getEngineService(conf);
		AuthenticationService authService = _engineService.getAuthenticationService();
		this._principal = authService.login(login, password);
	}

	public void createAttributesAndGroups(File file, int sheetNumber)
			throws CheckedException, BiffException, IOException {

		ObjectFactory factory = _engineService.getObjectFactory();
		UserService userService = _engineService.getUserService();
		GroupService groupService = _engineService.getGroupService();
		AttributeDefinitionService attributeService = _engineService.getAttributeDefinitionService();
		WorkbookSettings ws = new WorkbookSettings();
		ws.setEncoding("Cp1252");
		final Workbook xlsWorkbook = Workbook.getWorkbook(file, ws);
		final Sheet xlsSheet = xlsWorkbook.getSheet(sheetNumber);

		final int rows = xlsSheet.getRows();

		for (int r = 1; r < rows; r++) {

			final int colNumForType = 0;	
			final String type = xlsSheet.getCell(colNumForType, r).getContents();

			if ("ATTRIBUT".equalsIgnoreCase(type)) {

				System.out.println("\nCreation of an attribute");
				try {
					createAttribute(_principal, factory, attributeService, xlsSheet.getCell(1, r).getContents(),
							xlsSheet.getCell(2, r).getContents(), xlsSheet.getCell(4, r).getContents());
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}

			}
			if ("USER".equalsIgnoreCase(type)) {
				
				//mapping the column number to the user information
				final int colNumForLastName = 1;
				final int colNumForFirstName = 2;		
				final int colNumForLogin = 3;
				final int colNumForPwd = 4;
				final int colNumForEmail = 5;
				final int colNumForLanguage = 6;
				final int colNumForAttribut = 7;
				
				//get content of the column
				final String firstName = xlsSheet.getCell(colNumForFirstName, r).getContents();
				final String lastName = xlsSheet.getCell(colNumForLastName, r).getContents();
				final String email = xlsSheet.getCell(colNumForEmail, r).getContents();
				final String login = xlsSheet.getCell(colNumForLogin, r).getContents();
				final String pwd = xlsSheet.getCell(colNumForPwd, r).getContents();
				final String locale = xlsSheet.getCell(colNumForLanguage, r).getContents();
				final String attributes = xlsSheet.getCell(colNumForAttribut, r).getContents();
				String[] listAttributes = convertContentToArray(attributes);
				
				System.out.println("\nCreation of a user --> Prefer the other creation option");

				try {
					
					//create the properties
					HashMap<String, Object> properties = new HashMap<String, Object>();
					properties.put(UserPropertyKey.FIRST_NAME, firstName);
					properties.put(UserPropertyKey.LAST_NAME, lastName);
					properties.put(UserPropertyKey.EMAIL, email);

					// create the locale for the user
					Locale userLocal = Locale.ENGLISH;
					Locale[] locales = Locale.getAvailableLocales();
					for(int i = 0; i< locales.length; i++)
					{
						Locale availableLocale = locales[i];
						if (availableLocale.equals(new Locale(locale, "")))
						{
							userLocal = availableLocale;
							break;
						}
					}
					LanguageIdentifier languageIdentifier = factory.newLanguageIdentifier();
					languageIdentifier.setLocale(userLocal);

					//create the user
					UserIdentifier myUser = userService.createUser(_principal, null, login, pwd, languageIdentifier,
							properties, true);
					
					//add attributes to the user
					for (String attribute : listAttributes) {
						if (attribute != null && !attribute.isEmpty())
						{
							String[] values = attribute.split(":");
							AttributeDefinitionIdentifier attributeDefinition = getCreateAttributeDefinition(_principal,
									factory, attributeService, values[0], values[1], null);
							userService.addUserAttribute(_principal, myUser, attributeDefinition, values[2]);
						}
					}

				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}

			if ("GROUP".equalsIgnoreCase(type)) {
				System.out.println("\nCreation of a group");

				//mapping the column number to the group information
				final int colNumForgroupName = 1;
				final int colNumForgroupParent = 2;		
				final int colNumForAttribut = 3;
				
				//get content of the column
				final String groupName = xlsSheet.getCell(colNumForgroupName, r).getContents();
				final String groupParentName = xlsSheet.getCell(colNumForgroupParent, r).getContents();
				final String attributes = xlsSheet.getCell(colNumForAttribut, r).getContents();
				String[] listAttributes = convertContentToArray(attributes);
				
				try {
					GroupIdentifier myGroup = null;
					if ("".equals(groupParentName)) {
						myGroup = groupService.createGroup(_principal, null, groupName,
								null);
					} else {
						GroupIdentifier groupParentId = factory.newGroupIdentifier();
						groupParentId.setName(groupParentName);
						myGroup = groupService.createGroup(_principal, groupParentId,
								groupName, null);
					}

					//add attributes to the group
					for (String attribute : listAttributes) {
						if (attribute != null && !attribute.isEmpty())
						{
							String[] values = attribute.split(":");
							AttributeDefinitionIdentifier attributeDefinition = getCreateAttributeDefinition(_principal,
									factory, attributeService, values[0], values[1], null);
							groupService.addGroupAttribute(_principal, myGroup, attributeDefinition, values[2]);
						}
					}

				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			if ("ASSIGNATION".equalsIgnoreCase(type)) {
				System.out.println("\nGroup affectation to : " + xlsSheet.getCell(1, r).getContents());

				//mapping the column number to the assignation information
				final int colNumForUserLogin = 1;
				final int colNumForGroupNames = 2;		
				
				//get content of the column
				final String userLogin = xlsSheet.getCell(colNumForUserLogin, r).getContents();
				final String groupNames = xlsSheet.getCell(colNumForGroupNames, r).getContents();
				String[] listGroup = convertContentToArray(groupNames);
				
				try {
					UserIdentifier user = factory.newUserIdentifier();
					user.setName(userLogin);
					for (String groupName : listGroup) {
						if (groupName != null && !groupName.isEmpty())
						{
							GroupIdentifier groupIdentifier = factory.newGroupIdentifier();
							groupIdentifier.setName(groupName);
							userService.addUserToGroup(_principal, user, groupIdentifier);								
						}
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		}
		System.out.println("done.");
	}
	
	public boolean isList(String content)
	{
		return Pattern.matches("^\\[.*\\]$", content);
	}
	
	public String[] convertContentToArray(String content)
	{
		String[] contentAsArray;
		if(isList(content))
		{
			String contentStripped = content.substring(1, content.length() - 1);
			contentAsArray = contentStripped.split(";");
		}
		else
		{
			contentAsArray = new String[1];
			contentAsArray[0] = content;
		}
		return contentAsArray;
	}
}
