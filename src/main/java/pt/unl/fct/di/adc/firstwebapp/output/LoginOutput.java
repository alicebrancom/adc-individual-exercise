package pt.unl.fct.di.adc.firstwebapp.output;

import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;

public class LoginOutput {
    public AuthToken token;

    public LoginOutput() {}

    public LoginOutput(AuthToken token) {
        this.token = token;
    }
}