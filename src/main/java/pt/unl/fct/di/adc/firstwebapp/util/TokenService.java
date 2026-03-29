package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.output.ErrorMessage;
import pt.unl.fct.di.adc.firstwebapp.output.Errors;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TokenService {
    private Datastore datastore;
    private static final Logger LOG = Logger.getLogger(TokenService.class.getName());

    public TokenService(Datastore datastore) {
        this.datastore = datastore;
    }

    public Response validateToken(AuthToken t) {
        if (emptyOrBlankField(t.tokenId) || emptyOrBlankField(t.username) || emptyOrBlankField(t.role)
                || emptyOrBlankField(String.valueOf(t.issuedAt)) || emptyOrBlankField(String.valueOf(t.expiresAt))) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        try {
            Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(t.tokenId);
            Entity token = datastore.get(tokenKey);

            if (token == null) {
                ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
                return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
            }

            String user = token.getString("token_user");
            String role = token.getString("token_role");
            long issuedAt = token.getLong("token_issuedAt");
            long expiresAt = token.getLong("token_expiresAt");

            if (!t.username.equals(user) || !t.role.equals(role) ||
                    t.issuedAt != issuedAt || t.expiresAt != expiresAt) {
                ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
                return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
            }

            if (isExpired(token)) {
                ErrorMessage msg = new ErrorMessage(Errors.TOKEN_EXPIRED);
                return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
            }

            return Response.ok().build();
        } catch (Exception e) {
            LOG.severe("Error validating token: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error validating token.").build();
        }
    }

    public boolean isExpired(Entity token) {
        long currentTime = System.currentTimeMillis() / 1000;
        return token.getLong("token_expiresAt") <= currentTime;
    }

    public Entity getTokenEntity(AuthToken t) {
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(t.tokenId);
        Entity token = datastore.get(tokenKey);
        return token;
    }

    public List<Entity> getUserTokens(String username) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.eq("token_user", username))
                .build();

        QueryResults<Entity> userTokens = datastore.run(query);

        List<Entity> list = new ArrayList<>();
        while (userTokens.hasNext()) {
            list.add(userTokens.next());
        }
        return list;
    }

    private boolean emptyOrBlankField(String field) {
        return field == null || field.isBlank();
    }
}