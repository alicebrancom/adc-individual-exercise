package pt.unl.fct.di.adc.firstwebapp.util;

public class LogoutData {
    public Input input;
    public AuthToken token;

    public LogoutData() {}

    public static class Input {
        public String username;
        public Input() {}
    }
}