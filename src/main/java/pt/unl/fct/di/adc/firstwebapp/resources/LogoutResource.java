package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.Produces;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.output.*;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/logout")
public class LogoutResource {
    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public LogoutResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response doLogout(LogoutData data) {
        String username = data.input.username;
        LOG.fine("Attempt to logout user: " + username);

        TokenService tokenService = new TokenService(datastore);
        Response response = tokenService.validateToken(data.token);

        if (response.hasEntity()) {
            return response;
        }

        try {
            Transaction txn = datastore.newTransaction();

            String tokenUser = data.token.username;
            String tokenRole = data.token.role;

            if ((tokenRole.equals("USER") || tokenRole.equals("BOFFICER")) &&
                    !username.equals(tokenUser)) {
                txn.rollback();
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Entity tokenEntity = tokenService.getTokenEntity(data.token);

            if (tokenRole.equals("USER") || tokenRole.equals("BOFFICER")) {
                txn.delete(tokenEntity.getKey());
            }
            else { // ADMIN
                if (tokenUser.equals(username)) {

                    txn.delete(tokenEntity.getKey());
                }
                else {
                    List<Entity> userTokens = tokenService.getUserTokens(username);
                    for (Entity token : userTokens) {
                        txn.delete(token.getKey());
                    }
                }
            }
            txn.commit();

            SuccessMessage msg = new SuccessMessage(new Message("Logout successful"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            LOG.severe("Error logging out user: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error logging out user.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}