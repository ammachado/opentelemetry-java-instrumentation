/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachecamel.decorators;

class HttpsSpanDecorator extends HttpSpanDecorator {
  @Override
  protected String getProtocol() {
    return "https";
  }
}
