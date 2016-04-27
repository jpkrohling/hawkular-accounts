/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.accounts.backend.test;/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.UUID;

import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.hawkular.accounts.api.CurrentUser;
import org.hawkular.accounts.api.UserService;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Persona;
import org.jboss.logging.Logger;

/**
 * @author Juraci Paixão Kröhling
 */
@Singleton
@ApplicationScoped
public class ProducersForTest {
    // one for the whole application, for testing
    private UUID userId = UUID.randomUUID();
    private HawkularUser hawkularUser = new HawkularUser(userId, userId.toString());
    private Persona persona = hawkularUser;

    @Inject
    UserService userService;

    @Produces @Alternative
    public SessionContext getSessionContext() {
        SessionContext sessionContext = mock(SessionContext.class);
        when(sessionContext.getCallerPrincipal()).thenReturn(getPrincipal());
        return sessionContext;
    }

    @Produces @Alternative
    public Principal getPrincipal() {
        Logger.getLogger(this.getClass()).debug("----------- producing principal: " + getPrincipal());
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(userId.toString());
        return principal;
    }

    @Produces @CurrentUser @Alternative
    public HawkularUser getHawkularUser() {
        // we ensure the user exists on the DB before returning to the caller
        return userService.getOrCreateByIdAndName(hawkularUser.getId(), hawkularUser.getName());
    }

    public void setHawkularUser(HawkularUser hawkularUser) {
        this.hawkularUser = hawkularUser;
    }

    @Produces @Alternative
    public Persona getPersona() {
        Logger.getLogger(this.getClass()).debug("----------- producing persona: " + persona);
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }
}
