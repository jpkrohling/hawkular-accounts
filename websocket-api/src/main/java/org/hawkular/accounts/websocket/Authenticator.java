/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.accounts.websocket;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.Session;

import org.hawkular.accounts.api.PersonaService;
import org.hawkular.accounts.api.UserService;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Persona;
import org.hawkular.accounts.common.TokenVerifier;
import org.hawkular.accounts.common.UsernamePasswordConversionException;
import org.hawkular.accounts.common.UsernamePasswordConverter;
import org.hawkular.accounts.websocket.internal.CachedSession;

/**
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class Authenticator {

    @Inject
    UsernamePasswordConverter usernamePasswordConverter;

    @Inject
    TokenVerifier tokenVerifier;

    @Inject
    PersonaService personaService;

    @Inject
    UserService userService;

    private Map<String, CachedSession> cachedSessions = new HashMap<>();

    public void authenticate(String message, Session session) throws WebsocketAuthenticationException {
        try (JsonReader jsonReader = Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = jsonReader.readObject();
            JsonObject jsonAuth = jsonMessage.getJsonObject("authentication");

            // do we have this session on the cache?
            CachedSession cachedSession = cachedSessions.get(session.getId());

            if (null == jsonAuth && cachedSession != null) {
                // no "authentication" node in JSON, but the session has been previously authenticated, so, it's all ok
                return;
            }

            // if we don't have a session already or if it's not valid anymore, we get a new session based
            // on the contents of the message
            if (null == cachedSession || !isValid(cachedSession, jsonAuth)) {
                try {
                    cachedSession = authenticateWithMessage(jsonAuth);
                } catch (UsernamePasswordConversionException e) {
                    throw new WebsocketAuthenticationException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // cached session is valid!
            if (null != cachedSession) {
                this.cachedSessions.put(session.getId(), cachedSession);
            } else {
                // not that I'm trying to be rude, but...
                throw new WebsocketAuthenticationException("No authentication data provided.");
            }
        }
    }

    private CachedSession authenticateWithMessage(JsonObject jsonAuth) throws Exception {
        String personaId = jsonAuth.getString("persona");

        // now, we have either a "token" or a "login" object
        String authToken = jsonAuth.getString("token");
        if (null != authToken) {
            return authenticateWithToken(authToken, personaId);
        }

        // now, we have either a "token" or a "login" object
        JsonObject jsonLogin = jsonAuth.getJsonObject("login");
        if (null != jsonLogin) {
            String username = jsonLogin.getString("username");
            String password = jsonLogin.getString("password");
            return authenticateWithUsernamePassword(username, password, personaId);
        }

        return null;
    }

    private CachedSession authenticateWithUsernamePassword(String username, String password, String personaId) throws
            Exception {
        if (null == username || username.isEmpty()) {
            return null;
        }

        if (null == password || password.isEmpty()) {
            return null;
        }

        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private CachedSession authenticateWithToken(String authToken, String personaId) throws Exception {
        if (null == authToken) {
            return null;
        }

        String accessToken = tokenVerifier.verify(authToken);
        JsonReader jsonReader = Json.createReader(new StringReader(accessToken));
        JsonObject accessTokenJson = jsonReader.readObject();

        String userId = accessTokenJson.getString("sub");

        if (null == userId || userId.isEmpty()) {
            throw new IllegalStateException("Subject wasn't returned by the authentication server.");
        }

        long expirationTime = accessTokenJson.getInt("exp");
        expirationTime*= 1000;

        HawkularUser actualUser = userService.getOrCreateById(userId);

        Persona persona;
        if (null != personaId && !personaId.equals(userId)) {
            Persona personaToCheck = personaService.get(personaId);
            if (null == personaToCheck) {
                // persona was not found!
                throw new WebsocketAuthenticationException("Persona not found.");
            }

            if (personaService.isAllowedToImpersonate(actualUser, personaToCheck)) {
                persona = personaToCheck;
            } else {
                throw new WebsocketAuthenticationException("User is not allowed to impersonate this persona.");
            }
        } else {
            persona = actualUser;
        }

        return new CachedSession(accessToken, persona, expirationTime);
    }

    private boolean isValid(CachedSession cachedSession, JsonObject jsonAuth) {
        if (!cachedSession.getPersona().getId().equals(jsonAuth.getString("persona"))) {
            // session is for a different persona, force a new authentication
            return false;
        }

        return System.currentTimeMillis() < cachedSession.getExpiresAt();
    }
}
