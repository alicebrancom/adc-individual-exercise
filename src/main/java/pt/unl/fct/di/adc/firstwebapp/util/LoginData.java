package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginData {
	public String username;
	public String password;

	public LoginData() {}

	public LoginData(String userId, String password) {
		this.username = userId;
		this.password = password;
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validLogin() {
		return nonEmptyOrBlankField(username) && nonEmptyOrBlankField(password);
	}
}