#!/bin/bash
set -x
awslocal sns create-topic --name intervention-events-local
awslocal sqs create-queue --queue-name delius-intervention-events-queue
awslocal sns subscribe \
  --topic arn:aws:sns:eu-west-2:000000000000:intervention-events-local \
  --protocol sqs \
  --notification-endpoint arn:aws:sns:eu-west-2:000000000000:delius-intervention-events-queue
set +x