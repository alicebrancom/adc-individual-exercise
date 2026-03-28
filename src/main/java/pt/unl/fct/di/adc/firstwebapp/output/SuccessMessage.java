package pt.unl.fct.di.adc.firstwebapp.output;

public class SuccessMessage<T> {
    public String status = "success";
    public T data;

    public SuccessMessage(T data) {
        this.data = data;
    }
}