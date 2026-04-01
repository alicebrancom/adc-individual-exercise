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

import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.firstwebapp.output.*;
import pt.unl.fct.di.adc.firstwebapp.util.*;

@Path("/changeuserpwd")
public class ChangePasswordResource {
    private static final Logger LOG = Logger.getLogger(ChangePasswordResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ChangePasswordResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeUserPassword(ChangePasswordData data) {
        String username = data.input.username;
        LOG.fine("Attempt to change user password: " + username);

        Transaction txn = datastore.newTransaction();
        try {
            TokenService tokenService = new TokenService(datastore);
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            if (!username.equals(data.token.username)) {
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            String inputOldPwd = DigestUtils.sha512Hex(data.input.oldPassword);

            if (user == null || !user.getString("user_pwd").equals(inputOldPwd)) {
                ErrorMessage msg = new ErrorMessage(Errors.INVALID_CREDENTIALS);
                return Response.ok(g.toJson(msg)).build();
            }

            String inputNewPwd = DigestUtils.sha512Hex(data.input.newPassword);
            Entity updated = Entity.newBuilder(user).set("user_pwd", inputNewPwd).build();
            txn.put(updated);
            txn.commit();

            LOG.info("User password changed: " + username);
            SuccessMessage msg = new SuccessMessage(new Message("Password changed successfully"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            LOG.severe("Error changing user password: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error changing user password.").build();
        }
        finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}