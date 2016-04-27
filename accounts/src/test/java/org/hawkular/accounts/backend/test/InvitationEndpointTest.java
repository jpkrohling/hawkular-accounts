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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;

import org.hawkular.accounts.api.CurrentUser;
import org.hawkular.accounts.api.OrganizationService;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Organization;
import org.hawkular.accounts.api.model.Visibility;
import org.hawkular.accounts.backend.boundary.InvitationEndpoint;
import org.hawkular.accounts.backend.entity.rest.InvitationRequest;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * @author Juraci Paixão Kröhling
 */
@RunWith(Arquillian.class)
public class InvitationEndpointTest extends BaseEndpointTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    @Inject
    InvitationEndpoint endpoint;

    @Inject
    OrganizationService organizationService;

    @Inject @CurrentUser
    HawkularUser user;

    @Test
    public void inviteUserToOrganization() throws IOException, MessagingException {
        Logger.getLogger(this.getClass()).debug("----------- The user for this test is: " + user);
        Organization organization = organizationService.createOrganization(
                UUID.randomUUID().toString(),
                "acme",
                Visibility.APPLY,
                user
        );
        InvitationRequest request = new InvitationRequest();
        request.setEmails("jdoe@example.org,jdoe2@example.org");
        request.setOrganizationId(organization.getId());
        Response response = endpoint.inviteUserToOrganization(request);
        assertEquals(204, response.getStatus());
        assertTrue(greenMail.waitForIncomingEmail(5000, 1)); // 5 seconds timeout to receive one message

        MimeMessage message = greenMail.getReceivedMessages()[0];
        String body = (String) ((MimeMultipart)message.getContent()).getBodyPart(1).getContent();

        Pattern pattern = Pattern.compile("[a-z]+:\\/\\/[^ \\n]*");
        Matcher matcher = pattern.matcher(body);
        assertTrue(matcher.find());

        String url = body.substring(matcher.start(), matcher.end());
        String token = url.substring(url.lastIndexOf('/')+1).trim();

        Pattern uuidPattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        assertTrue(uuidPattern.matcher(token).matches());
    }

}
