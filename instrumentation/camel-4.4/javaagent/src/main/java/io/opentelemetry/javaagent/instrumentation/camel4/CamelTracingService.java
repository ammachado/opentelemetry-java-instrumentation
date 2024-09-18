/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Includes work from:
/*
 * Apache Camel Opentracing Component
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.javaagent.instrumentation.camel4;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.StaticService;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.service.ServiceHelper;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;

public class CamelTracingService extends ServiceSupport
    implements RoutePolicyFactory, StaticService {

  private final CamelContext camelContext;
  private final CamelEventNotifier eventNotifier = new CamelEventNotifier();
  private final CamelRoutePolicy routePolicy = new CamelRoutePolicy();

  public CamelTracingService(CamelContext camelContext) {
    ObjectHelper.notNull(camelContext, "CamelContext", this);
    this.camelContext = camelContext;
  }

  @Override
  protected void doStart() {
    camelContext.getManagementStrategy().addEventNotifier(eventNotifier);
    if (!camelContext.getRoutePolicyFactories().contains(this)) {
      camelContext.addRoutePolicyFactory(this);
    }

    ServiceHelper.startService(eventNotifier);
  }

  @Override
  protected void doStop() {
    // stop event notifier
    camelContext.getManagementStrategy().removeEventNotifier(eventNotifier);
    ServiceHelper.stopService(eventNotifier);

    // remove route policy
    camelContext.getRoutePolicyFactories().remove(this);
  }

  @Override
  public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
    return routePolicy;
  }
}
