# 2. Transition to decoupled events

Date: 2021-10-12

## Status

Draft

## Context

The [Interventions Service](https://github.com/ministryofjustice/hmpps-interventions-service) already handles several integrations with nDelius in a synchronous way.
If we switch to using this events listener to decouple these integrations from the Interventions Service without careful thought, it is likely we will encounter either missed or duplicate event processing.

## Decision

## Consequences



