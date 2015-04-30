package eu.w4.ps.genActor.excel;

import java.util.HashMap;

public class UserBPMN {


	private String password;
	private String login;
	private HashMap<String, Object> properties = new HashMap<String,Object>();
	private HashMap<String, String> attributes = new HashMap<String, String>(); 


	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public HashMap<String, Object> getProperties() {
		return properties;
	}


	public HashMap<String, String> getAttributes() {
		return attributes;
	}

	public void setProperties(String name, Object value) {
		this.properties.put(name, value);
	}


	public void setAttributes(String name, String value) {
		this.attributes.put(name, value);
	}


}
