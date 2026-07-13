# delivery-platform-distributed-system Architecture

## 1. Vision and Scope

This project is an educational distributed systems platform that models operational failures in a large-scale food delivery system.

The main business problem is **Pickup Conflict Resolution**.

A real-world failure scenario:

1. A customer places an order.
2. The restaurant prepares the order.
3. The platform assigns Driver A.
4. Due to a race condition, delayed event, reassignment issue, or human error, another driver receives the order.
5. Driver A arrives at the restaurant and discovers the order is already gone.
6. Driver A loses time, fuel, and effort.
7. Driver A reports the problem.
8. The platform reconstructs what happened using distributed events.
9. Support reviews the incident.
10. If the driver is eligible, compensation is processed.
11. The platform reaches a consistent final state.

The goal is not to build a food delivery clone. The goal is to demonstrate how distributed systems handle:

* inconsistent state
* race conditions
* operational failures
* human decision workflows
* compensation processes
* eventual consistency

## 2. Technical Goals

Technologies:

* Java 21
* Spring Boot
* Apache Kafka
* PostgreSQL
* Redis
* Docker
* Kubernetes
* OpenTelemetry
* Testcontainers

Distributed systems patterns:

* Event-driven architecture
* Transactional Outbox Pattern
* Saga Pattern
* Idempotent consumers
* Optimistic locking
* Eventual consistency
* Distributed tracing
* Retry strategies
* Dead Letter Queues
* Reconciliation processes
* Audit/event timeline reconstruction

---

# 3. Microservices and Bounded Contexts

The initial system contains five services.

## 1. Order Service

Owns:

* order lifecycle
* order state machine
* order consistency rules

Responsibilities:

* create orders
* track order status
* consume driver and dispatch events
* publish order events
* maintain order history

Database:

PostgreSQL

---

## 2. Dispatch Service

Owns:

* driver assignment
* reassignment decisions
* assignment consistency

Responsibilities:

* assign available drivers
* prevent duplicate assignments
* publish assignment events
* handle assignment failures

Database:

PostgreSQL

---

## 3. Driver Service

Owns:

* driver workflow
* driver availability
* arrival and pickup actions

Responsibilities:

* track driver status
* publish location updates
* report arrival
* confirm pickup
* report pickup conflicts

Database:

PostgreSQL

---

## 4. Incident Service

Owns delivery conflict cases.

This is the core domain introduced by this project.

Responsibilities:

* create incidents
* collect evidence
* maintain investigation timeline
* track resolution status
* connect events from multiple services

Example incident:

```
Incident:
INC-10001

Problem:
Pickup Conflict

Driver:
Driver A

Evidence:
- Driver assigned timestamp
- Driver arrival timestamp
- Pickup completed by another driver
- Order event history

Decision:
Approved

Compensation:
Processed
```

Database:

PostgreSQL

---

## 5. Support and Compensation Service

Owns human review workflow.

Responsibilities:

* review incidents
* evaluate evidence
* approve or reject compensation
* trigger compensation workflow
* close incidents

Important:

The system does not automatically decide compensation.

It provides evidence and workflow support for human decisions.

Database:

PostgreSQL

---

# 4. Event-Driven Architecture

Apache Kafka is the communication backbone.

Services communicate through events instead of direct database access.

Example:

```
Order Service
      |
      |
OrderCreated
      |
      v
Kafka
      |
      |
Dispatch Service

      |
      |
DriverAssigned

      |
      |
Driver Service

      |
      |
PickupConflictReported

      |
      |
Incident Service

      |
      |
Support Review

      |
      |
Compensation Processed
```

---

# 5. Core Domain Events

## Order Events

* OrderCreated
* OrderAccepted
* OrderReady
* DriverAssignmentRequested
* OrderCancelled
* OrderClosed

## Dispatch Events

* DriverAssigned
* DriverReassigned
* AssignmentFailed
* DuplicateAssignmentDetected

## Driver Events

* DriverEnRoute
* DriverArrived
* PickupConfirmed
* PickupConflictReported
* DriverOffline

## Incident Events

* IncidentCreated
* EvidenceCollected
* InvestigationStarted
* CompensationRequested
* CompensationApproved
* CompensationRejected
* IncidentClosed

---

# 6. Incident Resolution Workflow

The main saga:

```
PickupConflictReported

        |

IncidentCreated

        |

EvidenceCollected

        |

InvestigationStarted

        |

SupportDecisionMade

        |

CompensationRequested

        |

CompensationProcessed

        |

IncidentClosed
```

This models a real operational workflow where technical state and human decisions interact.

---

# 7. Event Timeline and Audit

A major requirement is reconstructing what happened.

Example:

```
10:01 OrderCreated

10:15 RestaurantReady

10:16 DriverAssigned

10:25 DriverArrived

10:26 AnotherDriverPickupConfirmed

10:27 PickupConflictReported

10:35 InvestigationStarted

10:45 CompensationApproved

10:50 IncidentClosed
```

Every event contains:

* eventId
* eventType
* aggregateId
* timestamp
* correlationId
* causationId
* version
* payload

This allows distributed debugging and audit.

---

# 8. Reconciliation Service

Large distributed systems cannot rely only on real-time events.

A reconciliation process periodically detects inconsistencies.

Examples:

```
Driver A assigned

BUT

Driver B completed pickup
```

or:

```
Order status:
ASSIGNED

Pickup event:
COMPLETED
```

The reconciliation service:

* compares system states
* detects anomalies
* creates incidents automatically
* triggers investigation workflows

---

# 9. Reliability Patterns

## Transactional Outbox

Each service writes:

1. business state change
2. event record

in the same database transaction.

Example:

```
Order updated

+

OrderCreated event stored

=

Database transaction committed
```

Then the publisher sends the event to Kafka.

---

## Idempotency

Duplicate events are expected.

Each consumer stores:

```
processed_events

event_id
processed_time
```

Repeated events are ignored safely.

---

## Optimistic Locking

Entities contain version numbers.

Example:

```
Order version = 5

Update allowed only if version = 5
```

Prevents lost updates during concurrent changes.

---

# 10. Project Development Plan

## Phase 1

Build:

* Order Service
* Kafka integration
* PostgreSQL
* Flyway migrations
* Outbox Pattern
* State machine
* Integration tests

## Phase 2

Build:

* Dispatch Service
* Driver assignment workflow
* Duplicate assignment prevention

## Phase 3

Build:

* Driver Service
* Pickup conflict detection

## Phase 4

Build:

* Incident Service
* Support workflow
* Compensation saga

## Phase 5

Add:

* Reconciliation
* OpenTelemetry
* Prometheus
* Grafana
* Kubernetes deployment

---

# Project Goal

This project demonstrates how modern backend systems handle failures caused by distributed execution.

The focus is not only making transactions succeed.

The focus is:

* detecting failures,
* understanding what happened,
* recovering safely,
* compensating affected users,
* maintaining consistency across distributed services.
