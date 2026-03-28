package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {
    public Input input;

    public RegisterData() {}

    public static class Input {
        public String username;
        public String password;
        public String confirmation;
        public String phone;
        public String address;
        public String role;
    }

    private boolean nonEmptyOrBlankField(String field) {
        return field != null && !field.isBlank();
    }

    public boolean validRegistration() {
        return nonEmptyOrBlankField(input.username) &&
                nonEmptyOrBlankField(input.password) &&
                nonEmptyOrBlankField(input.confirmation) &&
                nonEmptyOrBlankField(input.phone) && nonEmptyOrBlankField(input.address) &&
                nonEmptyOrBlankField(input.role) &&
                input.password.equals(input.confirmation) &&
                (input.role.equals("USER") || input.role.equals("BOFFICER") || input.role.equals("ADMIN"));
    }
}