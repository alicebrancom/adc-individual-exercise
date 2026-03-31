package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.cloud.datastore.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.output.ErrorMessage;
import pt.unl.fct.di.adc.firstwebapp.output.Errors;

import java.util.ArrayList;
import java.util.List;

public class TokenService {
    private Datastore datastore;

    public TokenService(Datastore datastore) {
        this.datastore = datastore;
    }

    public Response validateToken(AuthToken t, Transaction txn) {
        if (emptyOrBlankField(t.tokenId) || emptyOrBlankField(t.username) || emptyOrBlankField(t.role)
                || emptyOrBlankField(String.valueOf(t.issuedAt)) || emptyOrBlankField(String.valueOf(t.expiresAt))) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        Key tokenKey = datastore.newKeyFactory()
                .addAncestor(PathElement.of("User", t.username))
                .setKind("Token").newKey(t.tokenId);
        Entity token = txn.get(tokenKey);

        if (token == null) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        String role = token.getString("token_role");
        long issuedAt = token.getLong("token_issuedAt");
        long expiresAt = token.getLong("token_expiresAt");

        if (!t.role.equals(role) || t.issuedAt != issuedAt || t.expiresAt != expiresAt) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        if (isExpired(token)) {
            ErrorMessage msg = new ErrorMessage(Errors.TOKEN_EXPIRED);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        return Response.ok().build();
    }

    public boolean isExpired(Entity token) {
        long currentTime = System.currentTimeMillis() / 1000;
        return token.getLong("token_expiresAt") <= currentTime;
    }

    public QueryResults<Entity> getUserTokens(Key userKey, Transaction txn) {
        Query<Entity> query = Query.newEntityQueryBuilder()
                .setKind("Token")
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(userKey))
                .build();

        QueryResults<Entity> tokens = txn.run(query);
        return tokens;
    }

    private boolean emptyOrBlankField(String field) {
        return field == null || field.isBlank();
    }
}