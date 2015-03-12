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
package org.hawkular.accounts.sample.boundary;

import org.hawkular.accounts.api.CheckPermission;
import org.hawkular.accounts.api.PermissionChecker;
import org.hawkular.accounts.api.ResourceId;
import org.hawkular.accounts.api.ResourceService;
import org.hawkular.accounts.api.SkipPermissionCheck;
import org.hawkular.accounts.api.model.HawkularUser;
import org.hawkular.accounts.api.model.Resource;
import org.hawkular.accounts.sample.control.HawkularAccountsSample;
import org.hawkular.accounts.sample.entity.Sample;
import org.hawkular.accounts.sample.entity.SampleRequest;
import org.hawkular.accounts.sample.entity.Sample_;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * @author jpkroehling
 */
@Path("/samples")
@PermitAll
@Stateless
@CheckPermission
public class SampleService {
    @Inject @HawkularAccountsSample
    EntityManager em;

    @Inject
    HawkularUser currentUser;

    @Inject
    PermissionChecker permissionChecker;

    @Inject
    ResourceService resourceService;

    @GET
    public Response getAllSamples() {
        // CheckPermission will not act on methods without @ResourceId, as the method should take care of
        // retrieving all the data related to this user/org by itself
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Sample> query = builder.createQuery(Sample.class);
        Root<Sample> root = query.from(Sample.class);
        query.select(root);
        query.where(builder.equal(root.get(Sample_.ownerId), currentUser.getId()));

        return Response.ok().entity(em.createQuery(query).getResultList()).build();
    }

    @GET
    @Path("{sampleId}")
    @SkipPermissionCheck
    public Response getSample(@PathParam("sampleId") String sampleId) {
        // on this case, we would want to do it by ourselves, for some reason
        Sample sample = em.find(Sample.class, sampleId);
        Resource resource = resourceService.get(sampleId);
        if (permissionChecker.hasAccessTo(currentUser, resource)) {
            return Response.ok().entity(sample).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    public Response createSample(SampleRequest request) {
        // no permission checking?
        Sample sample = new Sample(UUID.randomUUID().toString(), currentUser.getId());
        resourceService.create(sample.getId());
        sample.setName(request.getName());

        em.persist(sample);
        return Response.ok().entity(sample).build();
    }

    @DELETE
    @Path("{sampleId}")
    public Response removeSample(@ResourceId @PathParam("sampleId") String sampleId) {
        // permission checking will happen for this one, so, once this method is called, the permission has been
        // checked already
        em.remove(em.find(Sample.class, sampleId));
        return Response.noContent().build();
    }
}
