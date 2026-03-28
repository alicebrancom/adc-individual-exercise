package pt.unl.fct.di.adc.firstwebapp.util;

public class ChangeRoleData {
    public Input input;
    public AuthToken token;

    public ChangeRoleData() {}

    public static class Input {
        public String username;
        public String newRole;

        public Input() { }
    }
}