/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachecamel.aws;

import com.google.common.util.concurrent.Futures;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class AwsConnector {
  private static final Logger logger = LoggerFactory.getLogger(AwsConnector.class);
  private final SqsAsyncClient sqsClient;
  private final S3Client s3Client;
  private final SnsAsyncClient snsClient;
  private final SQSRestServer sqsRestServer;

  AwsConnector(
      SqsAsyncClient sqsClient,
      S3Client s3Client,
      SnsAsyncClient snsClient,
      SQSRestServer sqsRestServer) {
    this.sqsRestServer = sqsRestServer;
    this.sqsClient = sqsClient;
    this.s3Client = s3Client;
    this.snsClient = snsClient;
  }

  static AwsConnector elasticMq() {
    int sqsPort = PortUtils.findOpenPort();
    SQSRestServer sqsRestServer =
        SQSRestServerBuilder.withPort(sqsPort).withInterface("localhost").start();

    AwsCredentialsProvider credentials =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("x", "x"));

    SqsAsyncClient sqsClient =
        SqsAsyncClient.builder()
            .credentialsProvider(credentials)
            .endpointOverride(URI.create("http://localhost:" + sqsPort))
            .build();

    return new AwsConnector(sqsClient, null, null, sqsRestServer);
  }

  static AwsConnector liveAws() {

    SqsAsyncClient sqsClient = SqsAsyncClient.builder().region(Region.US_EAST_1).build();

    S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();

    SnsAsyncClient snsClient = SnsAsyncClient.builder().region(Region.US_EAST_1).build();

    return new AwsConnector(sqsClient, s3Client, snsClient, null);
  }

  void createBucket(String bucketName) {
    logger.info("Create bucket {}", bucketName);
    s3Client.createBucket(builder -> builder.bucket(bucketName));
  }

  void deleteBucket(String bucketName) {
    logger.info("Delete bucket {}", bucketName);
    ListObjectsResponse objectListing = s3Client.listObjects(builder -> builder.bucket(bucketName));
    for (S3Object s3ObjectSummary : objectListing.contents()) {
      s3Client.deleteObject(builder -> builder.bucket(bucketName).key(s3ObjectSummary.key()));
    }
    s3Client.deleteBucket(builder -> builder.bucket(bucketName));
  }

  void enableS3ToSqsNotifications(String bucketName, String sqsQueueArn) {
    logger.info("Enable notification for bucket {} to queue {}", bucketName, sqsQueueArn);

    QueueConfiguration queueConfiguration =
        QueueConfiguration.builder().queueArn(sqsQueueArn).events(Event.S3_OBJECT_CREATED).build();

    NotificationConfiguration notificationConfiguration =
        NotificationConfiguration.builder().queueConfigurations(queueConfiguration).build();

    s3Client.putBucketNotificationConfiguration(
        builder -> builder.bucket(bucketName).notificationConfiguration(notificationConfiguration));
  }

  String getQueueArn(String queueUrl) {
    logger.info("Get ARN for queue {}", queueUrl);
    return Futures.getUnchecked(
            sqsClient.getQueueAttributes(
                builder -> builder.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN)))
        .attributes()
        .get(QueueAttributeName.QUEUE_ARN);
  }

  private static String getSqsPolicy(String resource) {
    return String.format(
        "{\"Statement\": [{\"Effect\": \"Allow\", \"Principal\": \"*\", \"Action\": \"sqs:SendMessage\", \"Resource\": \"%s\"}]}",
        resource);
  }

  void purgeQueue(String queueUrl) {
    logger.info("Purge queue {}", queueUrl);
    sqsClient.purgeQueue(builder -> builder.queueUrl(queueUrl));
  }

  void setQueuePublishingPolicy(String queueUrl, String queueArn) {
    logger.info("Set policy for queue {}", queueArn);
    Futures.getUnchecked(
        sqsClient.setQueueAttributes(
            builder ->
                builder
                    .queueUrl(queueUrl)
                    .attributes(
                        Collections.singletonMap(
                            QueueAttributeName.POLICY, getSqsPolicy(queueArn)))));
  }

  String createQueue(String queueName) {
    logger.info("Create queue {}", queueName);
    CompletableFuture<String> queue =
        sqsClient
            .createQueue(builder -> builder.queueName(queueName))
            .thenApply(CreateQueueResponse::queueUrl);
    return Futures.getUnchecked(queue);
  }

  void sendSampleMessage(String queueUrl) {
    Futures.getUnchecked(
        sqsClient.sendMessage(
            builder -> builder.queueUrl(queueUrl).messageBody("{\"type\": \"hello\"}")));
  }

  void receiveMessage(String queueUrl) {
    logger.info("Receive message from queue {}", queueUrl);
    ReceiveMessageResponse receiveMessageResult =
        Futures.getUnchecked(sqsClient.receiveMessage(builder -> builder.queueUrl(queueUrl)));
    for (Message ignored : receiveMessageResult.messages()) {}
  }

  void disconnect() {
    if (sqsRestServer != null) {
      sqsRestServer.stopAndWait();
    }
  }

  void publishSampleNotification(String topicArn) {
    Futures.getUnchecked(
        snsClient.publish(builder -> builder.topicArn(topicArn).message("Hello There")));
  }

  String createTopicAndSubscribeQueue(String topicName, String queueArn) {
    logger.info("Create topic {} and subscribe to queue {}", topicName, queueArn);
    CreateTopicResponse ctr =
        Futures.getUnchecked(snsClient.createTopic(builder -> builder.name(topicName)));
    Futures.getUnchecked(
        snsClient.subscribe(
            builder -> builder.topicArn(topicName).protocol("sqs").endpoint(queueArn)));
    return ctr.topicArn();
  }

  SqsAsyncClient getSqsClient() {
    return sqsClient;
  }

  S3Client getS3Client() {
    return s3Client;
  }

  SnsAsyncClient getSnsClient() {
    return snsClient;
  }
}
