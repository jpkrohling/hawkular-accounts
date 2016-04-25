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
package org.hawkular.accounts.api.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.ejb.SessionContext;

import org.hawkular.accounts.api.model.HawkularUser;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Juraci Paixão Kröhling
 */
public class UserServiceImplTest extends SessionEnabledTest {
    @Before
    public void addSessionContextToUserService() {
        SessionContext sessionContext = mock(SessionContext.class);
        when(sessionContext.getCallerPrincipal()).thenReturn(() -> "jdoe");
        userService.sessionContext = sessionContext;

    }

    @Test
    public void createUserOnDemandBasedOnCurrentUser() throws IOException {
        int numExistingUsers = userService.getAll().size();
        userService.getCurrent();

        List<HawkularUser> existingUsers = userService.getAll();
        assertEquals("There should 1 persona at the end of the test", numExistingUsers+1, existingUsers.size());
    }

    @Test
    public void createUserOnDemandBasedOnUserId() throws IOException {
        int numExistingUsers = userService.getAll().size();
        userService.getOrCreateById(UUID.randomUUID().toString());

        List<HawkularUser> existingUsers = userService.getAll();
        assertEquals("There should 1 persona at the end of the test", numExistingUsers+1, existingUsers.size());
    }

    @Test
    public void retrieveExistingUserById() throws IOException {
        String id = UUID.randomUUID().toString();
        userService.getOrCreateById(id);
        HawkularUser user = userService.getById(id);
        assertNotNull("User should exist", user);
    }

    @Test
    public void nonExistingUserReturnsNull() {
        HawkularUser user = userService.getById(UUID.randomUUID().toString());
        assertNull("User should not exist", user);
    }
}
