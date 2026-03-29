package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import pt.unl.fct.di.adc.firstwebapp.output.*;
import pt.unl.fct.di.adc.firstwebapp.util.*;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.gson.Gson;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();

	public LoginResource() {}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		String username = data.input.username;
		LOG.fine("Login attempt by user: " + username);

		if (!data.validLogin()) {
			ErrorMessage msg = new ErrorMessage(Errors.INVALID_INPUT);
			return Response.ok(g.toJson(msg)).build();
		}

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Transaction txn = datastore.newTransaction();

		try {
			Entity user = txn.get(userKey);
			if (user == null) {
				txn.rollback();
				ErrorMessage msg = new ErrorMessage(Errors.USER_NOT_FOUND);
				return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
			}

			String hashedPWD = user.getString("user_pwd");
			if (hashedPWD.equals(DigestUtils.sha512Hex(data.input.password))) {
				AuthToken token = new AuthToken(username, user.getString("user_role"));
				Key tokenKey = datastore.newKeyFactory()
						.setKind("Token")
						.newKey(token.tokenId);

				Entity tokenEntity = Entity.newBuilder(tokenKey)
								.set("token_id", token.tokenId)
						        .set("token_user", token.username)
						        .set("token_role", token.role)
						        .set("token_issuedAt", token.issuedAt)
						        .set("token_expiresAt", token.expiresAt)
						        .build();
				txn.put(tokenEntity);
				txn.commit();
				SuccessMessage msg = new SuccessMessage<>(new LoginOutput(token));
				LOG.info("Login successful by user: " + username);
				return Response.ok(g.toJson(msg)).build();
			}
			else {
				LOG.warning("Wrong password for: " + username);
				ErrorMessage msg = new ErrorMessage(Errors.INVALID_CREDENTIALS);
				return Response.ok(g.toJson(msg)).build();
			}

		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}