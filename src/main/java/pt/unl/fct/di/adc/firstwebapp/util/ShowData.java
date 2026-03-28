package pt.unl.fct.di.adc.firstwebapp.util;

public class ShowData {
    public Input input;
    public AuthToken token;

    public ShowData() {}

    public ShowData(Input input, AuthToken token) {
        this.input = input;
        this.token = token;
    }

    public static class Input {}
}
