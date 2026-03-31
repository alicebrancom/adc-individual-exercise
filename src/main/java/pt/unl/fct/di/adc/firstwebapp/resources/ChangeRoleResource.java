package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.List;
import java.util.logging.Logger;

import com.google.cloud.datastore.*;
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

        Transaction txn = datastore.newTransaction();
        try {
            TokenService tokenService = new TokenService(datastore);
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            String role = data.token.role;
            if (!role.equals("ADMIN")) {
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
                return Response.ok(g.toJson(msg)).build();
            }

            Entity updatedUser = Entity.newBuilder(user).set("user_role", data.input.newRole).build();
            txn.put(updatedUser);

            QueryResults<Entity> tokens = tokenService.getUserTokens(userKey, txn);
            while (tokens.hasNext()) {
                Entity updatedToken = Entity.newBuilder(tokens.next()).set("token_role", data.input.newRole).build();
                txn.put(updatedToken);
            }

            txn.commit();

            LOG.info("User role updated: " + username);
            SuccessMessage msg = new SuccessMessage(new Message("Role updated successfully"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            LOG.severe("Error changing user role: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user role.").build();
        }
        finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}