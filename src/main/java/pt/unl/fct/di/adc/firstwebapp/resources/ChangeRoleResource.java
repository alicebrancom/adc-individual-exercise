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

@Path("/changeuserrole")
public class ChangeRoleResource {
    private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ChangeRoleResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserRole(ChangeRoleData data) {
        String username = data.input.username;
        LOG.fine("Attempt to change user role: " + username);

        TokenService tokenService = new TokenService(datastore);
        Response response = tokenService.validateToken(data.token);

        if (response.hasEntity()) {
            return response;
        }

        try {
            Transaction txn = datastore.newTransaction();
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
                return Response.ok(g.toJson(msg)).build();
            }

            String role = user.getString("user_role");
            if (role.equals("USER") || role.equals("BOFFICER")) {
                txn.rollback();
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Entity updated = Entity.newBuilder(user).set("user_role", data.input.newRole).build();
            txn.put(updated);
            txn.commit();
            LOG.info("User role updated " + username);

            SuccessMessage msg = new SuccessMessage(new Message("Role updated successfully"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            LOG.severe("Error changing user role: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}