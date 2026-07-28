# Production Incident Scenarios

This project models production incidents commonly encountered in large-scale distributed systems. Each scenario demonstrates how failures are detected, investigated, and resolved while maintaining eventual consistency.

---

## Incident 1: Duplicate Driver Assignment

### Scenario

Due to a consumer rebalance, network delay, or concurrent processing, two dispatch workers assign different drivers to the same order.

### Symptoms

- Two `DriverAssigned` events exist for one order.
- Both drivers travel to the restaurant.
- One driver discovers the order has already been picked up.
- Customer support receives a complaint.

### Root Cause

Multiple consumers processed the same assignment request before the system reached a consistent state.

### Detection

- Duplicate assignment events detected.
- Optimistic locking conflict on the Order aggregate.
- Reconciliation job identifies conflicting assignments.

### Resolution

- Enforce optimistic locking.
- Use idempotent event consumers.
- Prevent duplicate active assignments with database constraints.
- Record the conflict as an Incident.

### Prevention

- Concurrency integration tests.
- Duplicate assignment metrics.
- Continuous reconciliation jobs.

---

## Incident 2: Lost Kafka Event

### Scenario

A consumer crashes after reading a `PickupConfirmed` event but before committing the offset.

### Symptoms

- Driver successfully completes pickup.
- Order remains in `ASSIGNED`.
- Incident timeline is incomplete.

### Resolution

- Transactional Outbox Pattern.
- Retry with exponential backoff.
- Dead Letter Queue after retry exhaustion.
- Reconciliation detects missing state transitions.

---

## Incident 3: Duplicate Event Delivery

### Scenario

Kafka delivers the same event more than once.

### Symptoms

- Multiple identical events arrive.
- Without protection, downstream state becomes inconsistent.

### Resolution

- Idempotent consumers.
- `processed_events` table.
- Ignore previously processed event IDs.

---

## Incident 4: Stuck Compensation Saga

### Scenario

Support approves compensation, but the Compensation Service is temporarily unavailable.

### Symptoms

- Incident remains in an intermediate state.
- Compensation request is never completed.

### Resolution

- Saga retries.
- Timeout detection.
- Automatic recovery worker.
- Manual intervention when retry limits are exceeded.

---

## Incident 5: Database and Kafka Inconsistency

### Scenario

Business data is committed successfully, but publishing the corresponding Kafka event fails.

### Resolution

- Transactional Outbox Pattern.
- Background publisher continuously retries until the event is delivered.
- No business transaction is lost.

---

## Incident 6: Out-of-Order Events

### Scenario

`PickupConfirmed` arrives before `DriverAssigned` because of network delays.

### Resolution

- Versioned aggregates.
- Event ordering validation.
- Temporary buffering when required.
- Reconciliation corrects remaining inconsistencies.

---

## Incident 7: Pickup Conflict

### Scenario

Driver A is assigned the order, but Driver B completes the pickup.

### Investigation Timeline

```
10:16 DriverAssigned (Driver A)

10:23 DriverReassigned (Driver B)

10:25 Driver B PickupConfirmed

10:27 Driver A Arrived

10:28 PickupConflictReported

10:35 InvestigationStarted

10:45 CompensationApproved
```

### Resolution

The Incident Service reconstructs the event timeline using:

- correlationId
- causationId
- aggregateId
- timestamps
- event history

Support reviews the evidence and determines whether compensation is appropriate.

---

# Reliability Objectives

| Category | Goal |
|-----------|------|
| Availability | 99.9% |
| Event Delivery | At-least-once |
| Consistency | Eventual Consistency |
| Recovery | Automatic where possible |
| Observability | OpenTelemetry + Prometheus + Grafana |
| Auditability | Complete event timeline reconstruction |

---

# Failure Injection & Chaos Testing

The platform intentionally injects failures to validate system resilience.

Planned failure scenarios include:

- Kafka consumer crashes
- Duplicate event delivery
- Network partitions
- Out-of-order events
- Database failures
- Service restarts
- Retry exhaustion
- Dead Letter Queue processing
- Optimistic locking conflicts
- Long-running Saga recovery

Each scenario is verified through automated integration tests using Testcontainers and production-like environments.
