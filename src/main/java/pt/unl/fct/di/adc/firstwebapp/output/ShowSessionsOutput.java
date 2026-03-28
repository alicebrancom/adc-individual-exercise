package pt.unl.fct.di.adc.firstwebapp.output;

import com.google.gson.JsonObject;
import java.util.List;

public class ShowSessionsOutput {
    public List<JsonObject> sessions;

    public ShowSessionsOutput(List<JsonObject> sessions) {
        this.sessions = sessions;
    }
}
