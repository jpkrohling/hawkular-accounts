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
package org.hawkular.accounts.sample.control;

import org.hawkular.accounts.rest.PermissionCheckerFilter;
import org.hawkular.accounts.sample.boundary.SampleService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration point with JAX-RS. Specifies that we have a JAX-RS application, on the namespace "/".
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
@ApplicationPath("/")
public class SampleApplication extends Application {
    private Set<Class<?>> classes = new HashSet<>();

    @Override
    public Set<Class<?>> getClasses() {
        classes.add(PermissionCheckerFilter.class);
        classes.add(SampleService.class);
        return classes;
    }
}
