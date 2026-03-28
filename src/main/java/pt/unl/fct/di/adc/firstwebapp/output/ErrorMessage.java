package pt.unl.fct.di.adc.firstwebapp.output;

public class ErrorMessage {
    public String status;
    public String data;

    public ErrorMessage(Errors error) {
        this.status = error.status;
        this.data = error.data;
    }
}
