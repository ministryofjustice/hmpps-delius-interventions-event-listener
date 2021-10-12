# 1. Error handling  

Date: 2021-10-12

## Status

Draft

## Context

Messages consumed from the queue have side effects in other HMPPS systems (e.g. nDelius). 
Sometimes these systems are unavailable or our requests to their APIs fail.
We need to define the logic and processes to recover from these failures and retry when appropriate.

## Decision

Messages will be put on a ['dead letter queue' (DLQ)](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-dead-letter-queues.html) after **two** failed processing attempts.
Trying to process the event twice should mainly rule out flaky networking or other random sources of failure.

When a message is placed on the DLQ an alert will be sent to Slack to notify the development team a message has failed to be processed.

## Consequences



