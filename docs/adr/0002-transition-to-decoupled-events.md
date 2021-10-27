# 2. Transition to decoupled events

Date: 2021-10-12

## Status

Proposed

## Context

The [Interventions Service](https://github.com/ministryofjustice/hmpps-interventions-service) already handles several integrations with nDelius in a synchronous way.
If we switch to using the Delius Interventions Event Listener to decouple these integrations from the Interventions Service without careful thought, it is likely we will encounter either missed or duplicate event processing.

## Decision

Since the creation of duplicate records in nDelius is a known issue within the Interventions Service, it has been decided to tackle this head on, making API calls in Community API idempotent.
This has the immediate benefit that duplicate event processing (both in Interventions Service and Delius Interventions Event Listener) is not harmful. 

On the issue of the accidental missing of published events, the only consideration is to ensure the event is being processed in the Delius Interventions Event Listener before the integration is disabled in Interventions Service.
Thus, there is in fact a _requirement_ that there is a period of 'duplicate event processing' (as discussed in the previous paragraph) as integrations are transitioned out of Interventions Service. 

## Consequences

There are no consequences for either Interventions Service or Delius Interventions Event Listener, other than a considered roll-out of integrations before they are disabled.

There are consequences for Community API, which must guarantee idempotence for relevant API calls; the details of this are outside the scope of this document.

