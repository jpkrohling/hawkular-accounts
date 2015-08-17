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
import javax.servlet.ServletContext;
import javax.websocket.Session;

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
    ServletContext servletContext;

    private Map<String, CachedSession> cachedSessions = new HashMap<>();

    public void authenticate(String message, Session session) throws WebsocketAuthenticationException {
        String keycloakAdapterConfig = servletContext.getInitParameter("org.keycloak.json.adapterConfig");

        // do we have this session on the cache?
        CachedSession cachedSession = cachedSessions.get(session.getId());

        // if we don't have a session already or if it's not valid anymore, we get a new session based
        // on the contents of the message
        if (null == cachedSession || !isValid(cachedSession)) {
            try {
                cachedSession = authenticateWithMessage(message);
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

    private CachedSession authenticateWithMessage(String message) throws Exception {
        JsonReader jsonReader = Json.createReader(new StringReader(message));
        JsonObject jsonMessage = jsonReader.readObject();

        JsonObject jsonAuth = jsonMessage.getJsonObject("authentication");
        if (null == jsonAuth) {
            // no "authentication" node in JSON, so, no CachedSession from it
            return null;
        }

        // now, we have either a "token" or a "login" object
        String authToken = jsonAuth.getString("token");
        if (null != authToken) {
            return authenticateWithToken(authToken);
        }

        // now, we have either a "token" or a "login" object
        JsonObject jsonLogin = jsonAuth.getJsonObject("login");
        if (null != jsonLogin) {
            String username = jsonLogin.getString("username");
            String password = jsonLogin.getString("password");
            return authenticateWithUsernamePassword(username, password);
        }

        jsonReader.close();
        return null;
    }

    private CachedSession authenticateWithUsernamePassword(String username, String password) throws Exception {
        if (null == username || username.isEmpty()) {
            return null;
        }

        if (null == password || password.isEmpty()) {
            return null;
        }

        throw new UnsupportedOperationException("Not implemented yet.");
    }

    private CachedSession authenticateWithToken(String authToken) {
        if (null == authToken) {
            return null;
        }

        return null;
    }

    private boolean isValid(CachedSession cachedSession) {
        return true;
    }
}
