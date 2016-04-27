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

import java.io.File;

import org.hawkular.accounts.api.InvitationService;
import org.hawkular.accounts.api.internal.SessionContextProducer;
import org.hawkular.accounts.api.internal.impl.InvitationServiceImpl;
import org.hawkular.accounts.api.model.Invitation;
import org.hawkular.accounts.backend.boundary.InvitationEndpoint;
import org.hawkular.accounts.backend.control.InvitationDispatcher;
import org.hawkular.accounts.backend.entity.InvitationCreatedEvent;
import org.hawkular.accounts.backend.entity.rest.InvitationAcceptRequest;
import org.hawkular.accounts.common.AuthServerRequestExecutor;
import org.hawkular.accounts.common.internal.CassandraSessionCallable;
import org.hawkular.commons.email.EmailDispatcher;
import org.hawkular.commons.email.internal.EmailDispatcherImpl;
import org.hawkular.commons.templates.TemplateService;
import org.hawkular.commons.templates.internal.TemplateServiceImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * @author Juraci Paixão Kröhling
 */
class BaseEndpointTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addPackage(InvitationEndpoint.class.getPackage())
                .addPackage(InvitationDispatcher.class.getPackage())
                .addPackage(InvitationCreatedEvent.class.getPackage())
                .addPackage(InvitationAcceptRequest.class.getPackage())
                .addPackage(InvitationService.class.getPackage())
                .addPackage(SessionContextProducer.class.getPackage())
                .addPackage(InvitationServiceImpl.class.getPackage())
                .addPackage(Invitation.class.getPackage())
                .addPackage(AuthServerRequestExecutor.class.getPackage())
                .addPackage(CassandraSessionCallable.class.getPackage())
                .addPackage(EmailDispatcherImpl.class.getPackage())
                .addPackage(EmailDispatcher.class.getPackage())
                .addPackage(TemplateServiceImpl.class.getPackage())
                .addPackage(TemplateService.class.getPackage())
                .addClass(ProducersForTest.class)
                .addClass(BaseEndpointTest.class)
                .addAsResource("hawkular_accounts.cql")
                // for our alternative session context producer
                .addAsWebInfResource("beans.xml", "beans.xml")

                // for cassandra driver
                .addAsWebInfResource("jboss-deployment-structure.xml", "jboss-deployment-structure.xml")

                // and some other dependencies
                .addAsLibraries(Maven
                        .resolver()
                        .resolve("org.mockito:mockito-all:1.10.19")
                        .withoutTransitivity()
                        .as(File.class))
                .addAsLibraries(Maven
                        .resolver()
                        .loadPomFromFile("pom.xml")
                        .resolve("com.icegreen:greenmail")
                        .withTransitivity()
                        .as(File.class))
                .addAsLibraries(Maven
                        .resolver()
                        .loadPomFromFile("pom.xml")
                        .resolve("org.freemarker:freemarker:2.3.23")
                        .withTransitivity()
                        .as(File.class));
    }
}
