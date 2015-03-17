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
package org.hawkular.accounts.api.internal.impl;

import org.hawkular.accounts.api.BaseEntityManagerEnabledTest;
import org.hawkular.accounts.api.PermissionChecker;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Organization;
import org.hawkular.accounts.api.model.Resource;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;

import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class PermissionCheckerImplTest extends BaseEntityManagerEnabledTest {
    @Test
    public void userDontHaveAccessToAnotherUsersResource() {
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Resource resource = new Resource(UUID.randomUUID().toString(), jsmith);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("User has access no access to another user's resource", !checker.hasAccessTo(jdoe, resource));
    }

    @Test
    public void ownerBelongsToOrganization() {
        HawkularUser user = new HawkularUser(UUID.randomUUID().toString());
        Organization organization = new Organization(UUID.randomUUID().toString(), user);
        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Owner of an organization should be a member of it", checker.isMemberOf(user, organization));
    }

    @Test
    public void organizationBelongsToOrganization() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // therefore, acme is a member of emca

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Organization owner of an organization should be a member of it", checker.isMemberOf(acme, emca));
    }

    @Test
    public void ownerBelongsToInnerOrganization() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // therefore, jdoe is a member of emca

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Owner of parent organization should be a member of it", checker.isMemberOf(jdoe, emca));
    }

    @Test
    public void siblingsDontBelongToEachOther() {
        // case here:
        // acme owns marketing
        // acme owns finance
        // jdoe is the owner of acme
        // finance DOES NOT BELONGS to marketing

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization finance = new Organization(UUID.randomUUID().toString(), acme);
        Organization marketing = new Organization(UUID.randomUUID().toString(), acme);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Siblings are not a member of each other", !checker.isMemberOf(marketing, finance));
        assertTrue("Siblings are not a member of each other", !checker.isMemberOf(finance, marketing));
    }

    @Test
    public void memberToOrganizationHasAccessToResource() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // jsmith is a member of acme
        // emca is the owner of resource 'metric1'
        // therefore, jsmith has access to the metric1 resource

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);
        acme.addMember(jsmith);

        Resource metric1 = new Resource(emca);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Owner of parent organization should be a member of it", checker.hasAccessTo(jsmith, metric1));
    }

    @Test
    public void resourceWithoutOwnerBelongsToParentsOwner() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // emca is the owner of resource 'node1'
        // 'memory1' is a sub resource of 'node1'
        // therefore, jdoe has access to the 'memory1' resource

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);

        Resource node1 = new Resource(emca);
        Resource memory1 = new Resource(node1);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Sub resource without an owner should be owned by the parent's owner", checker.hasAccessTo(jdoe,
                memory1));
    }

    @Test
    public void resourceWithOwnerBelongsToParentsOwner() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // jsmith is *not* member of acme
        // emca is the owner of resource 'node1'
        // 'database1' is a sub resource of 'node1'
        // therefore, jdoe has NO access to the 'database1' resource

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);

        Resource node1 = new Resource(emca);
        Resource database1 = new Resource(jsmith, node1);

        PermissionChecker checker = new PermissionCheckerImpl();
        assertTrue("Sub resource with an owner have independent rules", !checker.hasAccessTo(jdoe, database1));
    }

    @Test
    public void keycloakMemberToOrganizationHasAccessToResource() {
        // case here:
        // acme owns emca
        // jdoe is the owner of acme
        // jsmith is a member of acme (represented by a keycloak principal)
        // emca is the owner of resource 'metric1'
        // therefore, jsmith has access to the metric1 resource

        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());
        KeycloakPrincipal jsmithPrincipal = new KeycloakPrincipal(jsmith.getId(), null);
        Organization acme = new Organization(UUID.randomUUID().toString(), jdoe);
        Organization emca = new Organization(UUID.randomUUID().toString(), acme);
        acme.addMember(jsmith);

        Resource metric1 = new Resource(emca);

        PermissionCheckerImpl checker = new PermissionCheckerImpl();
        UserServiceImpl userService = new UserServiceImpl();
        userService.em = entityManager;
        checker.userService = userService;
        boolean hasAccess = checker.hasAccessTo(jsmithPrincipal, metric1);
        assertTrue("Owner of parent organization should be a member of it", hasAccess);
    }

}
