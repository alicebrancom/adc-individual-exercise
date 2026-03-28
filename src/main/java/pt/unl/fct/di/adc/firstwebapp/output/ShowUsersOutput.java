package pt.unl.fct.di.adc.firstwebapp.output;

import com.google.gson.JsonObject;
import java.util.List;

public class ShowUsersOutput {
    public List<JsonObject> users;

    public ShowUsersOutput(List<JsonObject> users) {
        this.users = users;
    }
}
