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
package org.hawkular.accounts.backend.control;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.hawkular.accounts.api.UserService;
import org.hawkular.accounts.api.internal.UserSettingsUpdatedEvent;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.UserSettings;

/**
 * @author Juraci Paixão Kröhling
 */
@Stateless
public class UserEmailUpdater {
    private static final String SETTINGS_KEY_EMAIL = "email";

    @Inject
    UserService userService;

    @Asynchronous
    public void updateUserEmail(@Observes UserSettingsUpdatedEvent event) {
        UserSettings settings = event.getSettings();
        if (settings.containsKey(SETTINGS_KEY_EMAIL)) {
            String email = settings.get(SETTINGS_KEY_EMAIL);
            HawkularUser user = settings.getUser();
            if (null != email && !email.equals(user.getEmail())) {
                user.setEmail(email);
                userService.update(user);
            }
        }

    }
}
