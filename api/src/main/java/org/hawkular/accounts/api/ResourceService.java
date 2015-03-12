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
package org.hawkular.accounts.api;

import org.hawkular.accounts.api.model.Owner;
import org.hawkular.accounts.api.model.Resource;

/**
 * Manages {@link org.hawkular.accounts.api.model.Resource}. A Resource can be anything that is meant to be protected
 * by the consumer modules. For instance, a Resource could be an Alert, an Inventory item or a Metric.
 *
 * Implementations of this interface should conform with CDI rules and be injectable into managed beans. For
 * consumers, it means that a concrete implementation of this interface can be injected via {@link javax.inject.Inject}
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public interface ResourceService {
    /**
     * Retrieves a {@link Resource} based on its ID.
     *
     * @param id the resource's ID
     * @return the existing {@link Resource} or null if the resource doesn't exists.
     */
    Resource get(String id);

    /**
     * Creates a {@link Resource} based on its ID, owned by the specified {@link org.hawkular.accounts.api.model.Owner}
     *
     * @param id    the resource's ID
     * @param owner if the resource doesn't exists, a new one is created with the specified owner
     * @return the newly created {@link Resource}
     */
    Resource create(String id, Owner owner);

    /**
     * Creates a {@link Resource} based on its ID, with the current user as owner.
     *
     * @param id    the resource's ID
     * @return the newly created {@link Resource}
     */
    Resource create(String id);

    /**
     * Removes a {@link Resource} based on its ID.
     *
     * @param id    the resource's ID
     */
    void delete(String id);
}
