# 2. Transition to decoupled events

Date: 2021-10-12

## Status

Proposed

## Context

The [Interventions Service](https://github.com/ministryofjustice/hmpps-interventions-service) already handles several integrations with nDelius in a synchronous way.
If we switch to using the Delius Event Listener to decouple these integrations from the Interventions Service without careful thought, it is likely we will encounter either missed or duplicate event processing.

## Decision

The Interventions Service will manage the roll-out of delius event processing by including a field in the event which instructs the Delius Event Listener whether to do anything.
In doing so, the Interventions Service can control exactly which events are processed 'internally' (within the Interventions Service) or 'externally' (within the Delius Event Listener).
This opens up the possibility to test new event processing logic on a small scale, as well as turning external event processing off entirely if needed.
It means the logic is self-contained within the Interventions Service and there is no sharing of configuration or synchronization to think about.

The field will be included in the `additionalInformation` section of the event DTO, and conceptually will indicate if the Interventions Service has already notified nDelius about the event.
The field will have the name `deliusIntegrationStatus` and will have an enum value of either `NOTIFIED` or `NOT_NOTIFIED`.

## Consequences

The inclusion of this additional field implies a contract between the Interventions Service and the Delius Event Listener.
For events where `deliusIntegrationStatus = NOT_NOTIFIED`, the Interventions Service is relying on a downstream consumer to notify nDelius.

In the future, when the Interventions Service is entirely absolved of responsibility to notify nDelius of events, the contract is void, and the responsibility lies entirely within the Delius Event Listener.

