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
package org.hawkular.accounts.rest;

import org.hawkular.accounts.api.CheckPermission;
import org.hawkular.accounts.api.PermissionChecker;
import org.hawkular.accounts.api.ResourceId;
import org.hawkular.accounts.api.ResourceService;
import org.hawkular.accounts.api.model.Resource;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.Collection;

/**
 * @author jpkroehling
 */
@Provider
public class PermissionCheckerFilter implements ContainerRequestFilter {
    @Context
    private ResourceInfo resourceInfo;

    @Inject
    ResourceService resourceService;

    @Inject
    PermissionChecker permissionChecker;

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        // first, we check if the package is annotated with @CheckPermission
        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass.getPackage().isAnnotationPresent(CheckPermission.class)) {
            doPermissionChecking(context);
        }

        // then, we check if the annotation is on the type
        if (resourceClass.isAnnotationPresent(CheckPermission.class)) {
            doPermissionChecking(context);
        }

        // last chance: is it on the method?
        if (resourceInfo.getResourceMethod().isAnnotationPresent(CheckPermission.class)) {
            doPermissionChecking(context);
        }
    }

    private void doPermissionChecking(ContainerRequestContext context) {
        Collection<String> propertyNames = context.getPropertyNames();
        for (Parameter parameter : resourceInfo.getResourceMethod().getParameters()) {
            if (parameter.isAnnotationPresent(ResourceId.class) && parameter.getType().equals(String.class)) {
                String resourceId = parameter.toString();
                doAccessCheckingByResource(resourceId, context);
            }
        }
    }

    private void doAccessCheckingByResource(String resourceId, ContainerRequestContext context) {
        Resource resource = resourceService.get(resourceId);

        if (resource == null) {
            // resource could not find (at least, not in Account's side)
            context.abortWith(Response.status(Response.Status.NOT_FOUND).build());
        }

        if (context.getMethod().equals(HttpMethod.DELETE) && !permissionChecker.isOwnerOf(resource)) {
            // only owners can delete resources
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }

        if (!permissionChecker.hasAccessTo(resource)) {
            context.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}
