package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.JsonObject;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;

import pt.unl.fct.di.adc.firstwebapp.util.*;
import pt.unl.fct.di.adc.firstwebapp.output.*;

@Path("/showusers")
public class ShowUsersResource {
    private static final Logger LOG = Logger.getLogger(ShowUsersResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ShowUsersResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showsUsers(ShowData data) {
        LOG.fine("Attempt to show users");

        TokenService tokenService = new TokenService(datastore);
        Response response = tokenService.validateToken(data.token);

        if (response.hasEntity()) {
            return response;
        }

        String role = data.token.role;
        if (role.equals("USER")) {
            ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
            return Response.ok(g.toJson(msg)).build();
        }

        try {
            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("User")
                    .build();

            QueryResults<Entity> users = datastore.run(query);

            List<JsonObject> list = new ArrayList<>();
            while (users.hasNext()) {
                Entity e = users.next();
                JsonObject user = new JsonObject();
                user.addProperty("username", e.getString("user_username"));
                user.addProperty("role", e.getString("user_role"));
                list.add(user);
            }

            SuccessMessage m = new SuccessMessage(new ShowUsersOutput(list));
            return Response.ok(g.toJson(m)).build();

        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error registering user.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}