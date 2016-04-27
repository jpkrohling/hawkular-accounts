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
package org.hawkular.accounts.backend.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.UUID;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;

import org.hawkular.accounts.api.PersonaService;
import org.hawkular.accounts.api.UserService;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Organization;
import org.hawkular.accounts.api.model.OrganizationJoinRequest;
import org.hawkular.accounts.api.model.Visibility;
import org.hawkular.accounts.backend.boundary.OrganizationEndpoint;
import org.hawkular.accounts.backend.boundary.OrganizationJoinEndpoint;
import org.hawkular.accounts.backend.boundary.OrganizationMembershipEndpoint;
import org.hawkular.accounts.backend.entity.rest.OrganizationJoinRequestDecisionRequest;
import org.hawkular.accounts.backend.entity.rest.OrganizationRequest;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Juraci Paixão Kröhling
 */
@RunWith(Arquillian.class)
public class OrganizationEndpointTest extends BaseEndpointTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    @Inject
    ProducersForTest producersForTest;

    @Inject
    OrganizationEndpoint organizationEndpoint;

    @Inject
    OrganizationMembershipEndpoint organizationMembershipEndpoint;

    @Inject
    OrganizationJoinEndpoint organizationJoinEndpoint;

    @Inject
    UserService userService;

    @Inject
    PersonaService personaService;

    @Test
    public void createOrganizationWithoutVisibility() {
        String orgName = UUID.randomUUID().toString();

        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        Response response = organizationEndpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization created = (Organization) response.getEntity();
        assertNotNull(created.getId());
        assertEquals("By default, the org should have been private", "PRIVATE", created.getVisibility().name());
        assertEquals("The company name should have been persisted", orgName, created.getName());
    }

    @Test
    public void createOrganizationWithApplyVisibility() {
        String orgName = UUID.randomUUID().toString();

        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.APPLY.name());

        Response response = organizationEndpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization created = (Organization) response.getEntity();
        assertNotNull(created.getId());
        assertEquals("By default, the org should have been private", "APPLY", created.getVisibility().name());
        assertEquals("The company name should have been persisted", orgName, created.getName());
    }

    @Test
    public void createOrganizationWithPrivateVisibility() {
        String orgName = UUID.randomUUID().toString();

        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.PRIVATE.name());

        Response response = organizationEndpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization created = (Organization) response.getEntity();
        assertNotNull(created.getId());
        assertEquals("By default, the org should have been private", "PRIVATE", created.getVisibility().name());
        assertEquals("The company name should have been persisted", orgName, created.getName());
    }

    @Test
    public void jsmithAppliesForOrganization() throws URISyntaxException, MessagingException {
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID(), "jdoe");
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID(), "jsmith");

        jdoe.setEmail("jdoe@example.com");
        jsmith.setEmail("jsmith@example.com");
        userService.update(jdoe);
        userService.update(jsmith);

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        String orgName = UUID.randomUUID().toString();
        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.APPLY.name());
        Response response = organizationEndpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization organization = (Organization) response.getEntity();
        assertEquals(jdoe.getId(), organization.getOwner().getId());

        producersForTest.setHawkularUser(jsmith);
        producersForTest.setPersona(jsmith);
        Response joinApplicationResponse = organizationJoinEndpoint.applyToJoin(organization.getId());
        OrganizationJoinRequest joinRequest = (OrganizationJoinRequest) joinApplicationResponse.getEntity();
        assertEquals(200, joinApplicationResponse.getStatus());
        assertNotNull(joinRequest.getId());
        assertEquals("PENDING", joinRequest.getStatus().name());
        assertTrue(greenMail.waitForIncomingEmail(5000, 2));

        // we expect two messages: one to the user who sent the join request, and one to the admin
        // both have the same subject... if the subject ever changes, we need to change the test as well
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            assertTrue("Expected to send a join request notification, but got another message",
                    message.getSubject().startsWith("[hawkular] - Join request for"));
        }

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);

        OrganizationJoinRequestDecisionRequest joinRequestDecisionRequest = new
                OrganizationJoinRequestDecisionRequest();
        joinRequestDecisionRequest.setDecision("ACCEPT");
        joinRequestDecisionRequest.setJoinRequestId(joinRequest.getId());
        Response requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        OrganizationJoinRequest joinRequestAfterDecision = (OrganizationJoinRequest) requestDecisionResponse
                .getEntity();
        assertEquals(200, requestDecisionResponse.getStatus());
        assertNotNull(joinRequestAfterDecision.getId());
        assertEquals("ACCEPTED", joinRequestAfterDecision.getStatus().name());
        assertTrue(greenMail.waitForIncomingEmail(5000, 2));
        MimeMessage message = greenMail.getReceivedMessages()[2];
        String expectedSubject = "[hawkular] - You have been accepted to join";
        assertTrue("Should have received an approval email", message.getSubject().startsWith(expectedSubject));

        assertTrue(personaService.isAllowedToImpersonate(jsmith, organization));
    }

    @Test
    public void jsmithGetsApprovedTwice() {
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID(), "jdoe");
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID(), "jsmith");

        jdoe.setEmail("jdoe.jsmithGetsApprovedTwice@example.com");
        jsmith.setEmail("jsmith.jsmithGetsApprovedTwice@example.com");
        userService.update(jdoe);
        userService.update(jsmith);

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        String orgName = UUID.randomUUID().toString();
        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.APPLY.name());
        Response response = organizationEndpoint.createOrganization(request);
        Organization organization = (Organization) response.getEntity();

        producersForTest.setHawkularUser(jsmith);
        producersForTest.setPersona(jsmith);
        Response joinApplicationResponse = organizationJoinEndpoint.applyToJoin(organization.getId());
        OrganizationJoinRequest joinRequestAfterDecision = (OrganizationJoinRequest) joinApplicationResponse.getEntity();

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        OrganizationJoinRequestDecisionRequest joinRequestDecisionRequest = new
                OrganizationJoinRequestDecisionRequest();
        joinRequestDecisionRequest.setDecision("ACCEPT");
        joinRequestDecisionRequest.setJoinRequestId(joinRequestAfterDecision.getId());

        Response requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(200, requestDecisionResponse.getStatus());

        requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(202, requestDecisionResponse.getStatus());

    }

    @Test
    public void jsmithGetsRejectedAndThenAccepted() {
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID(), "jdoe");
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID(), "jsmith");

        jdoe.setEmail("jdoe.jsmithGetsApprovedTwice@example.com");
        jsmith.setEmail("jsmith.jsmithGetsApprovedTwice@example.com");
        userService.update(jdoe);
        userService.update(jsmith);

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        String orgName = UUID.randomUUID().toString();
        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.APPLY.name());
        Response response = organizationEndpoint.createOrganization(request);
        Organization organization = (Organization) response.getEntity();

        producersForTest.setHawkularUser(jsmith);
        producersForTest.setPersona(jsmith);
        Response joinApplicationResponse = organizationJoinEndpoint.applyToJoin(organization.getId());
        OrganizationJoinRequest joinRequestAfterDecision = (OrganizationJoinRequest) joinApplicationResponse.getEntity();

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        OrganizationJoinRequestDecisionRequest joinRequestDecisionRequest = new
                OrganizationJoinRequestDecisionRequest();
        joinRequestDecisionRequest.setDecision("REJECT");
        joinRequestDecisionRequest.setJoinRequestId(joinRequestAfterDecision.getId());

        Response requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(200, requestDecisionResponse.getStatus());

        joinRequestDecisionRequest.setDecision("ACCEPT");
        requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(409, requestDecisionResponse.getStatus());

    }

    @Test
    public void jsmithGetsAcceptedAndThenRejected() {
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID(), "jdoe");
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID(), "jsmith");

        jdoe.setEmail("jdoe.jsmithGetsApprovedTwice@example.com");
        jsmith.setEmail("jsmith.jsmithGetsApprovedTwice@example.com");
        userService.update(jdoe);
        userService.update(jsmith);

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        String orgName = UUID.randomUUID().toString();
        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.APPLY.name());
        Response response = organizationEndpoint.createOrganization(request);
        Organization organization = (Organization) response.getEntity();

        producersForTest.setHawkularUser(jsmith);
        producersForTest.setPersona(jsmith);
        Response joinApplicationResponse = organizationJoinEndpoint.applyToJoin(organization.getId());
        OrganizationJoinRequest joinRequestAfterDecision = (OrganizationJoinRequest) joinApplicationResponse.getEntity();

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        OrganizationJoinRequestDecisionRequest joinRequestDecisionRequest = new
                OrganizationJoinRequestDecisionRequest();
        joinRequestDecisionRequest.setDecision("ACCEPT");
        joinRequestDecisionRequest.setJoinRequestId(joinRequestAfterDecision.getId());

        Response requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(200, requestDecisionResponse.getStatus());

        joinRequestDecisionRequest.setDecision("REJECT");
        requestDecisionResponse =  organizationJoinEndpoint.requestDecision(
                joinRequestDecisionRequest,
                organization.getId()
        );
        assertEquals(409, requestDecisionResponse.getStatus());

    }

}
