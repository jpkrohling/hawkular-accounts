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

import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Organization;
import org.hawkular.accounts.api.model.Visibility;
import org.hawkular.accounts.backend.boundary.OrganizationEndpoint;
import org.hawkular.accounts.backend.entity.rest.OrganizationRequest;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Juraci Paixão Kröhling
 */
@RunWith(Arquillian.class)
public class OrganizationEndpointTest extends BaseEndpointTest {

    @Inject
    OrganizationEndpoint endpoint;

    @ArquillianResource
    URL baseUrl;

    @Inject
    ProducersForTest producersForTest;

    @Test
    public void createOrganizationWithoutVisibility() {
        String orgName = UUID.randomUUID().toString();

        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        Response response = endpoint.createOrganization(request);
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

        Response response = endpoint.createOrganization(request);
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

        Response response = endpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization created = (Organization) response.getEntity();
        assertNotNull(created.getId());
        assertEquals("By default, the org should have been private", "PRIVATE", created.getVisibility().name());
        assertEquals("The company name should have been persisted", orgName, created.getName());
    }

    @Test
    public void jsmithAppliesForOrganization() throws URISyntaxException {
        HawkularUser jdoe = new HawkularUser(UUID.randomUUID().toString());
        HawkularUser jsmith = new HawkularUser(UUID.randomUUID().toString());

        producersForTest.setHawkularUser(jdoe);
        producersForTest.setPersona(jdoe);
        String orgName = UUID.randomUUID().toString();
        OrganizationRequest request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.PRIVATE.name());
        Response response = endpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        Organization created = (Organization) response.getEntity();
        assertEquals(jdoe.getId(), created.getOwner().getId());

        producersForTest.setHawkularUser(jsmith);
        producersForTest.setPersona(jsmith);
        orgName = UUID.randomUUID().toString();
        request = new OrganizationRequest();
        request.setName(orgName);
        request.setVisibility(Visibility.PRIVATE.name());
        response = endpoint.createOrganization(request);
        assertEquals(200, response.getStatus());
        created = (Organization) response.getEntity();
        assertEquals(jsmith.getId(), created.getOwner().getId());
    }

}
