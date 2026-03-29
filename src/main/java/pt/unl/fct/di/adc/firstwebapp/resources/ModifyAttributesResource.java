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

@Path("/modaccount")
public class ModifyAttributesResource {
    private static final Logger LOG = Logger.getLogger(ModifyAttributesResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    private final Gson g = new Gson();

    public ModifyAttributesResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyAttributes(ModifyAttributesData data) {
        String username = data.input.username;
        LOG.fine("Attempt to modify attributes: " + username);

        if (!data.validate()) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_INPUT);
            return Response.ok(g.toJson(msg)).build();
        }

        TokenService tokenService = new TokenService(datastore);
        Response response = tokenService.validateToken(data.token);

        if (response.hasEntity()) {
            return response;
        }

        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                txn.rollback();
                ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
                return Response.ok(g.toJson(msg)).build();
            }

            Entity t = tokenService.getTokenEntity(data.token);
            String tokenRole = t.getString("token_role"); // role of the account that will modify
            String tokenUser = t.getString("token_user"); // username of the account that will modify
            String userRole = user.getString("user_role"); // role of the account that will be modified

            if ((tokenRole.equals("USER") && !tokenUser.equals(username)) ||
                    (tokenRole.equals("BOFFICER") && !(userRole.equals("USER") || tokenUser.equals(username)))) {
                txn.rollback();
                ErrorMessage msg = new ErrorMessage(Errors.UNAUTHORIZED);
                return Response.ok(g.toJson(msg)).build();
            }

            Entity.Builder updated = Entity.newBuilder(user);
            String address = data.input.attributes.address;
            String phone = data.input.attributes.phone;

            if (address != null && !address.isBlank()) {
                updated.set("user_address", address);
            }

            if (phone != null && !phone.isBlank()) {
                updated.set("user_phone", phone);
            }

            txn.put(updated.build());
            txn.commit();
            LOG.info("Attributes modified: " + username);

            SuccessMessage msg = new SuccessMessage(new Message("Updated successfully"));
            return Response.ok(g.toJson(msg)).build();

        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Error modifying user attributes: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error modifying user attributes.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}