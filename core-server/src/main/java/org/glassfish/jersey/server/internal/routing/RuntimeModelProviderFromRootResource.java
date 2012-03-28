/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.internal.routing;

import javax.ws.rs.core.Request;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.internal.routing.RouterModule.RootRouteBuilder;
import org.glassfish.jersey.server.internal.routing.RouterModule.RouteToPathBuilder;
import org.glassfish.jersey.server.model.InflectorBasedResourceMethod;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.ResourceConstructor;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.SubResourceLocator;
import org.glassfish.jersey.server.model.SubResourceMethod;
import org.glassfish.jersey.uri.PathPattern;

import com.google.common.base.Function;

/**
 * Constructs runtime model for a root resource class.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RuntimeModelProviderFromRootResource extends RuntimeModelProviderBase {

    private String currentResourcePath = null;

    public RuntimeModelProviderFromRootResource() {
    }

    public RuntimeModelProviderFromRootResource(MessageBodyWorkers msgBodyWorkers) {
        super(msgBodyWorkers);
    }

    @Override
    TreeAcceptor adaptResourceMethodAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushResourceUriAndDelegateTreeAcceptor(injector, resource, acceptor);
    }

    @Override
    TreeAcceptor adaptSubResourceMethodAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushResourceUriAndDelegateTreeAcceptor(injector, resource, acceptor);
    }

    @Override
    TreeAcceptor adaptSubResourceLocatorAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushResourceUriAndDelegateTreeAcceptor(injector, resource, acceptor);
    }

    @Override
    TreeAcceptor adaptSubResourceAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushUriAndDelegateTreeAcceptor(injector, acceptor);
    }

    @Override
    TreeAcceptor createFinalTreeAcceptor(RootRouteBuilder<PathPattern> rootRouteBuilder, RouteToPathBuilder<PathPattern> lastRoutedBuilder) {
        final TreeAcceptor routingRoot;
        if (lastRoutedBuilder != null) {
            routingRoot = lastRoutedBuilder.build();
        } else {
            /**
             * Create an empty routing root that accepts any request, does not do
             * anything and does not return any inflector. This will cause 404 being
             * returned for every request.
             */
            routingRoot = Stages.acceptingTree(new Function<Request, Request>() {

                @Override
                public Request apply(Request input) {
                    return input;
                }

            }).build();
        }
        return rootBuilder.root(routingRoot);
    }


    @Override
    public void visitResourceClass(ResourceClass resource) {
        if (resource.isRootResource()) {
            currentResourcePath = resource.getPath().getValue();
        }
    }

    @Override
    public void visitResourceMethod(final ResourceMethod method) {
        if (method.getDeclaringResource().isRootResource()) {
            addMethodInflector(new PathPattern(currentResourcePath, PathPattern.RightHandPath.capturingZeroSegments), method, createInflector(method));
        }
    }

    @Override
    public void visitSubResourceMethod(SubResourceMethod method) {
        if (method.getDeclaringResource().isRootResource()) {
            addSubResourceLocatorEntry(currentResourcePath, new SubResourceMethodEntry(method.getHttpMethod(), method, createInflector(method)));
        }
    }

    @Override
    public void visitSubResourceLocator(SubResourceLocator locator) {
        if (locator.getResource().isRootResource()) {
            addSubResourceLocatorEntry(currentResourcePath,
                new SubResourceLocatorEntry(locator, new SubResourceLocatorAcceptor(injector, services, workers, locator)));
        }
    }

    @Override
    public void visitResourceConstructor(ResourceConstructor constructor) {
    }

    @Override
    public void visitInflectorResourceMethod(InflectorBasedResourceMethod method) {
        if (method.getDeclaringResource().isRootResource()) {
            addMethodInflector(new PathPattern(currentResourcePath, PathPattern.RightHandPath.capturingZeroSegments), method, method.getInflector());
        }
    }
}
