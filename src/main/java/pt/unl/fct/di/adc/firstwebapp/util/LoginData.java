package pt.unl.fct.di.adc.firstwebapp.util;

public class LoginData {
	public Input input;

	public LoginData() {}

	public static class Input {
		public String username;
		public String password;

		public Input() {}
	}

	private boolean nonEmptyOrBlankField(String field) {
		return field != null && !field.isBlank();
	}

	public boolean validLogin() {
		return nonEmptyOrBlankField(input.username) && nonEmptyOrBlankField(input.password);
	}
}