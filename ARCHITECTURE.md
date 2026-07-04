# 🚢 Shipping Load Verifier — Architecture & Design in Diagrams

Diagram-first reference for how the system works and the OOP / design patterns behind it. Every diagram below is GitHub-flavored Mermaid — copy the fenced block as-is.

**Shape legend**

```mermaid
flowchart LR
    A["App / process"]
    B{{"Exchange"}}
    C[("Queue")]
    D{"Decision"}
```
 
---

## 1. End-to-end message flow

```mermaid
flowchart LR
    DISP["DispatcherApp<br/>(Producer)"]
    IX{{"shipyard.ingest<br/>DIRECT"}}
    VQ[("load.verification")]
    VER["VerifierApp<br/>(Consumer + Producer)"]
    SX{{"shipyard.streams<br/>TOPIC"}}
    DLX{{"shipyard.rejected<br/>DEAD LETTER"}}
    RQ[("stream.rejected")]
    HQ[("stream.hazmat")]
    CQ[("stream.cold-chain")]
    OQ[("stream.special-handling")]
    CUQ[("stream.customs")]
    STQ[("stream.standard")]
    UQ[("stream.urgent-monitor")]
    WARE["WarehouseApp<br/>(7 StreamConsumers)"]
 
    DISP -->|"key: incoming"| IX --> VQ --> VER
    VER -->|"nack → invalid"| DLX --> RQ
    VER -->|"publish load.category.priority"| SX
    SX -->|"load.hazmat.*"| HQ
    SX -->|"load.refrigerated.*"| CQ
    SX -->|"load.oversized.*"| OQ
    SX -->|"load.international.*"| CUQ
    SX -->|"load.standard.*"| STQ
    SX -->|"load.*.urgent"| UQ
    HQ & CQ & OQ & CUQ & STQ & UQ & RQ --> WARE
```
 
---

## 2. Topic exchange — one message, multiple queues

An urgent hazmat load matches two bindings at once.

```mermaid
flowchart TD
    L["Load: hazardous + urgent<br/>routing key = load.hazmat.urgent"]
    SX{{"shipyard.streams (topic)"}}
    L --> SX
    SX ==>|"load.hazmat.* ✓"| HQ[("stream.hazmat")]
    SX ==>|"load.*.urgent ✓"| UQ[("stream.urgent-monitor")]
    SX -.->|"load.refrigerated.* ✗"| CQ[("stream.cold-chain")]
    SX -.->|"load.oversized.* ✗"| OQ[("stream.special-handling")]
    SX -.->|"load.standard.* ✗"| STQ[("stream.standard")]
```
 
---

## 3. Message lifecycle (valid vs invalid)

```mermaid
sequenceDiagram
    autonumber
    participant D as DispatcherApp
    participant B as RabbitMQ Broker
    participant V as VerifierApp
    participant W as StreamConsumer
 
    D->>B: publish → shipyard.ingest (key=incoming, persistent)
    B->>V: deliver from load.verification (autoAck=false)
    V->>V: LoadVerifier.verify() = chain + strategy
    alt load is valid
        V->>B: publish → shipyard.streams (load.category.priority)
        V->>B: basicAck (verification message)
        B->>W: deliver from matching stream(s)
        W->>W: process work
        W->>B: basicAck
    else load is invalid
        V->>B: basicNack(requeue=false)
        B->>B: dead-letter → shipyard.rejected
        B->>W: deliver from stream.rejected
        W->>B: basicAck
    end
```
 
---

## 4. Delivery acknowledgement states

Why manual ack matters: a crash before ack means redelivery, not data loss.

```mermaid
stateDiagram-v2
    [*] --> Ready
    Ready --> Unacked: delivered to consumer
    Unacked --> Acked: basicAck (success)
    Unacked --> DeadLettered: basicNack requeue=false
    Unacked --> Ready: consumer crash / channel closed
    Acked --> [*]
    DeadLettered --> [*]
```
 
---

## 5. Chain of Responsibility — verification pipeline

### 5a. Runtime flow (short-circuits on first failure)

```mermaid
flowchart LR
    IN["ShippingLoad"] --> C1
    C1{"LoadIdCheck<br/>id present?"} -->|fail| R["RoutingDecision.reject"]
    C1 -->|pass| C2{"DestinationCheck<br/>destination present?"}
    C2 -->|fail| R
    C2 -->|pass| C3{"WeightCheck<br/>weight &gt; 0?"}
    C3 -->|fail| R
    C3 -->|pass| OK["all checks passed<br/>→ classify"]
```

### 5b. Class structure

```mermaid
classDiagram
    class LoadCheck {
        <<abstract>>
        -LoadCheck next
        +linkTo(LoadCheck) LoadCheck
        +verify(ShippingLoad) Optional~String~
        #inspect(ShippingLoad) Optional~String~
    }
    class LoadIdCheck {
        #inspect(ShippingLoad) Optional~String~
    }
    class DestinationCheck {
        #inspect(ShippingLoad) Optional~String~
    }
    class WeightCheck {
        #inspect(ShippingLoad) Optional~String~
    }
    LoadCheck <|-- LoadIdCheck
    LoadCheck <|-- DestinationCheck
    LoadCheck <|-- WeightCheck
    LoadCheck --> LoadCheck : next
```
 
---

## 6. Strategy — swappable classification

```mermaid
classDiagram
    class ClassificationStrategy {
        <<interface>>
        +categorize(ShippingLoad) LoadCategory
    }
    class FlagBasedClassificationStrategy {
        +categorize(ShippingLoad) LoadCategory
    }
    class LoadVerifier {
        -LoadCheck checks
        -ClassificationStrategy strategy
        +verify(ShippingLoad) RoutingDecision
    }
    ClassificationStrategy <|.. FlagBasedClassificationStrategy
    LoadVerifier o-- ClassificationStrategy : strategy
    LoadVerifier o-- LoadCheck : checks (chain head)
```
 
---

## 7. Proxy — reliable publisher

### 7a. Class structure

```mermaid
classDiagram
    class LoadPublisher {
        <<interface>>
        +publish(exchange, routingKey, body)
    }
    class ChannelLoadPublisher {
        -Channel channel
        +publish(exchange, routingKey, body)
    }
    class ReliablePublisherProxy {
        -LoadPublisher delegate
        -AtomicLong publishedCount
        +publish(exchange, routingKey, body)
        +publishedCount() long
    }
    LoadPublisher <|.. ChannelLoadPublisher
    LoadPublisher <|.. ReliablePublisherProxy
    ReliablePublisherProxy o-- LoadPublisher : delegate
    ReliablePublisherProxy ..> ChannelLoadPublisher : wraps
```

### 7b. Call flow through the proxy

```mermaid
sequenceDiagram
    participant C as Caller (Verifier / Dispatcher)
    participant P as ReliablePublisherProxy
    participant R as ChannelLoadPublisher
    participant B as RabbitMQ
 
    C->>P: publish(exchange, key, body)
    alt body empty
        P-->>C: throw IllegalArgumentException
    else body ok
        P->>R: publish(exchange, key, body)
        R->>B: basicPublish(persistent, application/json)
        P->>P: publishedCount++ and log
        P-->>C: return
    end
```
 
---

## 8. OOP / package map

```mermaid
flowchart TB
    subgraph P_MODEL["model"]
        SL[ShippingLoad]
        LCAT[LoadCategory]
        PRI[Priority]
    end
    subgraph P_CHECK["verifier.check (Chain of Responsibility)"]
        LCK[LoadCheck]
        IDC[LoadIdCheck]
        DC[DestinationCheck]
        WC[WeightCheck]
    end
    subgraph P_VER["verifier (Strategy)"]
        LV[LoadVerifier]
        RD[RoutingDecision]
        CS[ClassificationStrategy]
        FB[FlagBasedClassificationStrategy]
        VA[VerifierApp]
    end
    subgraph P_PUB["publisher (Proxy)"]
        LP[LoadPublisher]
        CLP[ChannelLoadPublisher]
        RPP[ReliablePublisherProxy]
    end
    subgraph P_CONS["consumer"]
        SC[StreamConsumer]
        WA[WarehouseApp]
    end
    subgraph P_PROD["producer"]
        DA[DispatcherApp]
        SLo[SampleLoads]
    end
    subgraph P_CFG["config + util"]
        TOP[Topology]
        RC[RabbitConnection]
        JS[Json]
    end
 
    IDC -.extends.-> LCK
    DC -.extends.-> LCK
    WC -.extends.-> LCK
    FB -.implements.-> CS
    CLP -.implements.-> LP
    RPP -.implements.-> LP
    RPP --> LP
    LV --> LCK
    LV --> CS
    VA --> LV
    VA --> LP
    DA --> LP
    DA --> SLo
    WA --> SC
    VA --> TOP
    WA --> TOP
    DA --> TOP
    VA --> RC
```
 
---

## 9. Runtime & deployment

```mermaid
flowchart LR
    subgraph HOST["Your machine"]
        subgraph DK["Docker"]
            RMQ["shipyard-rabbitmq<br/>5672 (AMQP) / 15672 (UI)"]
        end
        JVM1["JVM: VerifierApp"]
        JVM2["JVM: WarehouseApp"]
        JVM3["JVM: DispatcherApp"]
        UI["Browser :15672"]
    end
    JVM1 <-->|"AMQP 5672"| RMQ
    JVM2 <-->|"AMQP 5672"| RMQ
    JVM3 -->|"AMQP 5672"| RMQ
    UI -->|"HTTP 15672"| RMQ
```
 
---

## 10. Prefetch / fair dispatch (QoS)

Run more than one verifier; `basicQos(1)` spreads work evenly.

```mermaid
flowchart LR
    VQ[("load.verification")] -->|"prefetch=1"| V1["VerifierApp #1"]
    VQ -->|"prefetch=1"| V2["VerifierApp #2"]
    VQ -->|"prefetch=1"| V3["VerifierApp #3"]
```
 
---

## 11. Object collaboration inside `VerifierApp`

```mermaid
flowchart LR
    MSG["incoming message"] --> VA["VerifierApp"]
    VA -->|"verify()"| LV["LoadVerifier"]
    LV -->|"1: run chain"| CHAIN["LoadCheck chain"]
    LV -->|"2: categorize()"| STRAT["ClassificationStrategy"]
    LV -->|"RoutingDecision"| VA
    VA -->|"accepted → publish()"| PROXY["ReliablePublisherProxy"]
    PROXY --> REAL["ChannelLoadPublisher"]
    REAL --> BROKER{{"shipyard.streams"}}
    VA -->|"rejected → basicNack"| DLXB{{"shipyard.rejected"}}
```