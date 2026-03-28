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

    public Response validateToken(AuthToken t) {
        if (emptyOrBlankField(t.tokenId) || emptyOrBlankField(t.username) || emptyOrBlankField(t.role)
                || emptyOrBlankField(String.valueOf(t.issuedAt)) || emptyOrBlankField(String.valueOf(t.expiresAt))) {
            System.err.println("DEBUG: Validation failed at Step 1 (Empty/Blank fields)");
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        Transaction txn = datastore.newTransaction();
        Key tokenKey = datastore.newKeyFactory().setKind("Token").newKey(t.tokenId);
        Entity token = txn.get(tokenKey);

        if (token == null) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        String user = token.getString("token_user");
        String role = token.getString("token_role");
        Long issuedAt = token.getLong("token_issuedAt");
        Long expiresAt = token.getLong("token_expiresAt");

        if (!t.username.equals(user) || !t.role.equals(role) ||
                !t.issuedAt.equals(issuedAt) || !t.expiresAt.equals(expiresAt)) {
            ErrorMessage msg = new ErrorMessage(Errors.INVALID_TOKEN);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        long currentTime = System.currentTimeMillis();
        if (expiresAt <= currentTime) {
            ErrorMessage msg = new ErrorMessage(Errors.TOKEN_EXPIRED);
            txn.delete(tokenKey);
            return Response.ok(msg).type(MediaType.APPLICATION_JSON).build();
        }

        txn.commit();
        return Response.ok().build();
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