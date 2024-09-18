/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camel4.decorators;

class Https4SpanDecorator extends HttpSpanDecorator {
  @Override
  protected String getProtocol() {
    return "https4";
  }
}
