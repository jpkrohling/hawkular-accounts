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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.hawkular.accounts.api.BaseEntityManagerEnabledTest;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Organization;
import org.hawkular.accounts.api.model.OrganizationMembership;
import org.hawkular.accounts.api.model.Role;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Juraci Paixão Kröhling
 */
public class OrganizationMembershipServiceImplTest extends BaseEntityManagerEnabledTest {

    OrganizationMembershipServiceImpl membershipService = new OrganizationMembershipServiceImpl();
    PersonaServiceImpl personaService = new PersonaServiceImpl();
    ResourceServiceImpl resourceService = new ResourceServiceImpl();

    @Before
    public void prepare() {
        resourceService.em = entityManager;
        membershipService.em = entityManager;
        personaService.em = entityManager;

        membershipService.personaService = personaService;
        membershipService.resourceService = resourceService;

        personaService.membershipService = membershipService;
    }

    @Test
    public void listMembershipsForUserNotBelongingToAnyOrganization() {
        entityManager.getTransaction().begin();
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        entityManager.persist(jdoe);
        entityManager.getTransaction().commit();

        List<OrganizationMembership> memberships = membershipService.getMembershipsForPersona(jdoe);
        assertEquals("The persona should not belong to any organizations", 0, memberships.size());
    }

    @Test
    public void listMembershipsForUserBelongingToOrganization() {
        entityManager.getTransaction().begin();
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(jdoe);
        Role role = new Role("SuperUser", "can do anything");
        OrganizationMembership membership = new OrganizationMembership(acme, jdoe, role);
        entityManager.persist(jdoe);
        entityManager.persist(acme);
        entityManager.persist(role);
        entityManager.persist(membership);
        entityManager.getTransaction().commit();

        List<OrganizationMembership> memberships = membershipService.getMembershipsForPersona(jdoe);
        assertEquals("There should be one membership for this persona", 1, memberships.size());
    }

    @Test
    public void listMembershipsForSoleOrganization() {
        entityManager.getTransaction().begin();
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(jdoe);
        Role role = new Role("SuperUser", "can do anything");
        OrganizationMembership membership = new OrganizationMembership(acme, jdoe, role);
        entityManager.persist(jdoe);
        entityManager.persist(acme);
        entityManager.persist(role);
        entityManager.persist(membership);
        entityManager.getTransaction().commit();

        List<OrganizationMembership> memberships = membershipService.getMembershipsForPersona(acme);
        assertEquals("There should be no memberships for this organization", 0, memberships.size());
    }

    @Test
    public void listMembershipsForOrganizationBelongingToOrganization() {
        entityManager.getTransaction().begin();
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(jdoe);
        Organization itDepartment = new Organization(acme);
        Role superUser = new Role("SuperUser", "can do anything");
        Role administrator = new Role("Administrator", "not quite everything");
        OrganizationMembership membershipAcme = new OrganizationMembership(acme, jdoe, superUser);
        OrganizationMembership membershipItDepartment = new OrganizationMembership(itDepartment, acme, superUser);

        entityManager.persist(jdoe);
        entityManager.persist(acme);
        entityManager.persist(itDepartment);
        entityManager.persist(superUser);
        entityManager.persist(administrator);
        entityManager.persist(membershipAcme);
        entityManager.persist(membershipItDepartment);
        entityManager.getTransaction().commit();

        List<OrganizationMembership> memberships = membershipService.getMembershipsForPersona(acme);
        assertEquals("Acme is super persona of IT department", 1, memberships.size());
    }

    @Test
    public void changeRole() {
        entityManager.getTransaction().begin();
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());
        Organization acme = new Organization(jdoe);
        Role superUser = new Role("SuperUser", "can do anything");
        Role monitor = new Role("Monitor", "almost nothing");

        entityManager.persist(jdoe);
        entityManager.persist(jsmith);
        entityManager.persist(acme);
        entityManager.persist(superUser);
        entityManager.persist(monitor);
        entityManager.getTransaction().commit();

        entityManager.getTransaction().begin();
        OrganizationMembership jsmithAtAcme = new OrganizationMembership(acme, jsmith, monitor);
        entityManager.persist(jsmithAtAcme);
        entityManager.getTransaction().commit();

        entityManager.getTransaction().begin();
        membershipService.changeRole(jsmithAtAcme, superUser);
        entityManager.getTransaction().commit();

        entityManager.getTransaction().begin();
        List<OrganizationMembership> memberships = membershipService.getPersonaMembershipsForOrganization(jsmith, acme);
        assertEquals("jsmith should be only super user at acme at this point", 1, memberships.size());
        assertEquals("jsmith should be only super user at acme at this point", "SuperUser",
                memberships.get(0).getRole().getName());
        entityManager.getTransaction().commit();

        entityManager.getTransaction().begin();
        membershipService.changeRole(jsmithAtAcme, monitor);
        entityManager.getTransaction().commit();

        entityManager.getTransaction().begin();
        memberships = membershipService.getPersonaMembershipsForOrganization(jsmith, acme);
        assertEquals("jsmith should be only monitor at acme at this point", 1, memberships.size());
        assertEquals("jsmith should be only monitor at acme at this point", "Monitor",
                memberships.get(0).getRole().getName());
        entityManager.getTransaction().commit();
    }
}
