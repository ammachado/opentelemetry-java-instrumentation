plugins {
  id("otel.java-conventions")
}

val camelVersion = "4.4.0" // first version that the tests pass on

dependencies {
  testImplementation(project(":instrumentation:camel-4.4:javaagent"))
  testImplementation(project(":instrumentation-api-incubator"))
  testImplementation(project(":javaagent-extension-api"))

  testImplementation("org.apache.camel:camel-core:$camelVersion")
  testImplementation("org.apache.camel:camel-aws2-sqs:$camelVersion")
  testImplementation("org.apache.camel:camel-aws-xray:$camelVersion")
  testImplementation("org.apache.camel:camel-http:$camelVersion")

  testImplementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
  testImplementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")
}
