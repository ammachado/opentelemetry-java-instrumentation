plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.apache.camel")
    module.set("camel-core")
    versions.set("[4.4.0,)")
    assertInverse.set(true)
  }
}

val camelversion = "4.4.0" // first version that the tests pass on

description = "camel-4.4"

dependencies {

  bootstrap(project(":instrumentation:executors:bootstrap"))

  library("org.apache.camel:camel-api:$camelversion")
  library("org.apache.camel:camel-support:$camelversion")
  implementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")

  compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testInstrumentation(project(":instrumentation:executors:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-5.0:javaagent"))
  testInstrumentation(project(":instrumentation:servlet:servlet-5.0:javaagent"))
  testInstrumentation(project(":instrumentation:aws-sdk:aws-sdk-2.2:javaagent"))

  testImplementation("org.apache.camel.springboot:camel-spring-boot-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-jetty-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-http-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-jaxb-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-undertow-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-aws2-s3-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-aws2-sns-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-aws2-sqs-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-aws-xray-starter:$camelversion")
  testImplementation("org.apache.camel.springboot:camel-cassandraql-starter:$camelversion")

  testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.9")
  testImplementation("org.springframework.boot:spring-boot-starter:3.2.9")

  testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
  testImplementation("org.elasticmq:elasticmq-rest-sqs_2.12:1.0.0")

  testImplementation("org.testcontainers:cassandra")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("com.datastax.oss:java-driver-core:4.16.0") {
    exclude(group = "io.dropwizard.metrics", module = "metrics-core")
  }

  latestDepTestLibrary("org.apache.camel:camel-api:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel:camel-support:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel:camel-core:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-spring-boot-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-jetty-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-http-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-jaxb-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-undertow-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-aws-aws2-s3-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-aws-xray-starter:4.+") // documented limitation
  latestDepTestLibrary("org.apache.camel.springboot:camel-cassandraql-starter:4.+") // documented limitation
}

tasks {
  withType<Test>().configureEach {
    // TODO run tests both with and without experimental span attributes
    jvmArgs("-Dotel.instrumentation.camel.experimental-span-attributes=true")
    jvmArgs("-Dotel.instrumentation.aws-sdk.experimental-span-attributes=true")

    // TODO: fix camel instrumentation so that it uses semantic attributes extractors
    jvmArgs("-Dotel.instrumentation.experimental.span-suppression-strategy=span-kind")

    // required on jdk17
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")

    jvmArgs("-Dotel.instrumentation.common.experimental.controller-telemetry.enabled=true")
  }
}

configurations.testRuntimeClasspath {
  resolutionStrategy {
    force("ch.qos.logback:logback-classic:1.4.14")
    force("org.slf4j:slf4j-api:2.0.12")
  }
}

otelJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}
