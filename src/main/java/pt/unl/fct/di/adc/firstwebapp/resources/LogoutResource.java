package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.Produces;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;

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
        String userToLogout = data.input.username;
        LOG.fine("Attempt to logout user: " + userToLogout);

        Transaction txn = datastore.newTransaction();
        try {
            TokenService tokenService = new TokenService(datastore);
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            String tokenUser = data.token.username;
            String tokenRole = data.token.role;

            if ((tokenRole.equals("USER") || tokenRole.equals("BOFFICER")) &&
                    !userToLogout.equals(tokenUser)) {
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Key tokenKey = datastore.newKeyFactory()
                    .addAncestor(PathElement.of("User", tokenUser))
                    .setKind("Token").newKey(data.token.tokenId);

            if (userToLogout.equals(tokenUser)) {
                txn.delete(tokenKey);
            }
            else {
                Key userToLogoutKey = datastore.newKeyFactory().setKind("User").newKey(userToLogout);
                QueryResults<Entity> tokens = tokenService.getUserTokens(userToLogoutKey, txn);
                while (tokens.hasNext()) {
                    txn.delete(tokens.next().getKey());
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
            if (txn.isActive()) txn.rollback();
        }
    }
}