package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangePasswordData {
    public Input input;
    public AuthToken token;

    public ChangePasswordData() {}

    public static class Input {
        public String username;
        public String oldPassword;
        public String newPassword;

        public Input() {}
    }
}