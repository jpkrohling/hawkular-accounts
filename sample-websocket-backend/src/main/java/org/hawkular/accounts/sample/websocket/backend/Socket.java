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
package org.hawkular.accounts.sample.websocket.backend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.hawkular.accounts.api.PersonaService;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Persona;

@ServerEndpoint("/socket")
public class Socket {
    Map<String, Persona> cachedSessions = new HashMap<>();

    @Inject
    PersonaService personaService;

    @OnMessage
    public String onMessage(String message, Session session) throws IOException {
        if (message.toLowerCase().startsWith("login:")) {
            doLogin(message, session);
        }
        return message;
    }

    private void doLogin(String message, Session session) throws IOException {
        HawkularUser user = null;
        String[] parts = message.split(":");

        if (parts.length != 2) {
            // should not happen, as it's checked already on the onMessage
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Please, login first."));
        }

//        String tokenPart = parts[1].trim();
//
//        if (user != null) {
//            cachedSessions.put(session.getId(), new HawkularUser("abc"));
//            return "Continue";
//        } else {
//            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Login failed."));
//        }
    }

    @OnOpen
    public String onOpen() {
        return "";
    }

}
