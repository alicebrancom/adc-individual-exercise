package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.ArrayList;
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
import pt.unl.fct.di.adc.firstwebapp.output.ErrorMessage;
import pt.unl.fct.di.adc.firstwebapp.output.Errors;
import pt.unl.fct.di.adc.firstwebapp.output.Message;
import pt.unl.fct.di.adc.firstwebapp.output.SuccessMessage;
import pt.unl.fct.di.adc.firstwebapp.util.TokenService;
import pt.unl.fct.di.adc.firstwebapp.util.DeleteData;

@Path("/deleteaccount")
public class DeleteResource {
    private static final Logger LOG = Logger.getLogger(DeleteResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public DeleteResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(DeleteData data) {
        String userToDelete = data.input.username;
        LOG.fine("Attempt to delete user: " + userToDelete);

        TokenService tokenService = new TokenService(datastore);
        Response response = tokenService.validateToken(data.token);

        if (response.hasEntity()) {
            return response;
        }

        Key userKey = datastore.newKeyFactory().setKind("User").newKey(userToDelete);
        Entity user = datastore.get(userKey);

        if (user == null) {
            ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
            return Response.ok(g.toJson(msg)).build();
        }
        
        String role = data.token.role;
        if (role.equals("USER") || role.equals("BOFFICER")) {
            ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
            return Response.ok(g.toJson(msg)).build();
        }

        try {
            List<Entity> userTokens = tokenService.getUserTokens(userToDelete);
            Transaction txn = datastore.newTransaction();

            for (Entity token : userTokens) {
                txn.delete(token.getKey());
            }

            txn.delete(userKey);
            txn.commit();
            LOG.info("User deleted " + userToDelete);

            SuccessMessage msg = new SuccessMessage(new Message("Account deleted successfully"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) { // forbidden
            LOG.severe("Error deleting user: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error deleting user.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}