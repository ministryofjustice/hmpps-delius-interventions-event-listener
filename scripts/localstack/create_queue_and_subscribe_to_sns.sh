#!/bin/bash
set -x
awslocal sqs create-queue --queue-name delius-intervention-dead-letter-queue
awslocal sqs create-queue --queue-name delius-intervention-events-queue \
  --attributes '{"VisibilityTimeout":"5","RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:delius-intervention-dead-letter-queue\",\"maxReceiveCount\":\"2\"}"}'

awslocal sns create-topic --name intervention-events-local
awslocal sns subscribe \
  --topic arn:aws:sns:eu-west-2:000000000000:intervention-events-local \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:delius-intervention-events-queue \
  --attributes '{"FilterPolicy": "{\"eventType\": [\"intervention.referral.assigned\", \"intervention.action-plan.submitted\"]}"}'
set +x

# AWS_DEFAULT_REGION=eu-west-2 awslocal sqs list-queues
# AWS_DEFAULT_REGION=eu-west-2 awslocal sqs purge-queue --queue-url http://localhost:4566/000000000000/delius-intervention-dead-letter-queue
# AWS_DEFAULT_REGION=eu-west-2 awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/delius-intervention-dead-letter-queue --max-number-of-messages 10
# AWS_DEFAULT_REGION=eu-west-2 awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/delius-intervention-events-queue --max-number-of-messages 10

