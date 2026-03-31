package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.Produces;
import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
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

@Path("/createaccount")
public class RegisterResource {
	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public RegisterResource() {}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerUser(RegisterData data) {
		String username = data.input.username;
		LOG.fine("Attempt to register user: " + username);

		if (!data.validRegistration()) {
			ErrorMessage msg = new ErrorMessage(Errors.INVALID_INPUT);
			return Response.ok(g.toJson(msg)).build();
		}

		String password = data.input.password;
		String role = data.input.role;
		String phone = data.input.phone;
		String address = data.input.address;

		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
			Entity user = txn.get(userKey);

			if (user != null) {
				ErrorMessage msg = new ErrorMessage(Errors.USER_ALREADY_EXISTS);
				return Response.ok(g.toJson(msg)).build();
			}

			else {
				user = Entity.newBuilder(userKey)
						.set("user_username", username)
						.set("user_pwd", DigestUtils.sha512Hex(password))
						.set("user_phone", phone)
						.set("user_address", address)
						.set("user_role", role)
						.build();

				txn.put(user);
				txn.commit();
				LOG.info("User registered " + username);

				SuccessMessage msg = new SuccessMessage(new RegisterOutput(username, role));
				return Response.ok(g.toJson(msg)).build();
			}

		} catch (Exception e) {
			LOG.severe("Error registering user: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error registering user.").build();
		}
		finally {
			if (txn.isActive()) txn.rollback();
		}
	}
}