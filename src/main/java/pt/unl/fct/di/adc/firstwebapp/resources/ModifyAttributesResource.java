package pt.unl.fct.di.adc.firstwebapp.resources;

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

        Transaction txn = datastore.newTransaction();
        try {
            TokenService tokenService = new TokenService(datastore);
            Response response = tokenService.validateToken(data.token, txn);

            if (response.hasEntity()) {
                return response;
            }

            Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
            Entity user = txn.get(userKey);

            if (user == null) {
                ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
                return Response.ok(g.toJson(msg)).build();
            }

            String userRole = user.getString("user_role"); // role of the account that will be modified
            String tokenRole = data.token.role; // role of the account that will modify
            String tokenUser = data.token.username; // username of the account that will modify

            if ((tokenRole.equals("USER") && !tokenUser.equals(username)) ||
                    (tokenRole.equals("BOFFICER") && !(userRole.equals("USER") || tokenUser.equals(username)))) {
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
            LOG.severe("Error modifying user attributes: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error modifying user attributes.").build();
        }
        finally {
            if (txn.isActive()) txn.rollback();
        }
    }
}