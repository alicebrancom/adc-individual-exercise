package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.output.*;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/showuserrole")
public class ShowRoleResource {
    private static final Logger LOG = Logger.getLogger(ShowRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ShowRoleResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showUserRole(ShowRoleData data) {
        String username = data.input.username;
        LOG.fine("Attempt to show user role: " + username);

        Transaction txn = datastore.newTransaction();
        try {
            TokenService tokenService = new TokenService(datastore);
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            String role = data.token.role;
            if (role.equals("USER")) {
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
                return Response.ok(g.toJson(msg)).build();
            }

            LOG.info("User role received " + username);
            SuccessMessage msg = new SuccessMessage(new ShowRoleOutput(username, user.getString("user_role")));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            LOG.severe("Error showing user role: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error showing user role.").build();
        }
        finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}