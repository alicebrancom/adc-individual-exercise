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

import pt.unl.fct.di.adc.firstwebapp.output.*;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/showauthsessions")
public class ShowSessionsResource {
    private static final Logger LOG = Logger.getLogger(ShowSessionsResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ShowSessionsResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showsSessions(ShowData data) {
        LOG.fine("Attempt to show sessions");

        TokenService tokenService = new TokenService(datastore);
        Transaction txn = datastore.newTransaction();
        try {
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            String role = data.token.role;
            if (!role.equals("ADMIN")) {
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
            }

            txn.rollback();

            Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Token")
                    .build();
            QueryResults<Entity> tokens = datastore.run(query);
            List<JsonObject> list = new ArrayList<>();

            while (tokens.hasNext()) {
                Entity e = tokens.next();
                if (!tokenService.isExpired(e)) {
                    JsonObject token = new JsonObject();
                    token.addProperty("tokenId", e.getString("token_id"));
                    token.addProperty("username", e.getString("token_user"));
                    token.addProperty("role", e.getString("token_role"));
                    token.addProperty("expiresAt", e.getLong("token_expiresAt"));
                    list.add(token);
                }
            }

            SuccessMessage m = new SuccessMessage(new ShowSessionsOutput(list));
            return Response.ok(g.toJson(m)).build();

        } catch (Exception e) {
            LOG.severe("Error showing authenticated sessions: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error showing authenticated sessions.").build();
        } finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}