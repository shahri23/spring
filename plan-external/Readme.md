# 🚀 EIP Demo Testing Guide

## 📁 File Structure Setup

Place this file in your system:
```
/tmp/
└── eip-advanced-patterns.xml     # All patterns in one XML

/tmp/input/                       # For file processing
/tmp/output/                      # For file output
```

## 🧪 Testing Each EIP Pattern

### 1️⃣ Basic Patterns

- **Message Enricher + Filter + Transformer**

```bash
curl -X POST http://localhost:8080/api/basic-transform \
  -H "Content-Type: text/plain" \
  -d "Hello World"
```
**Expected Output:**
```
Response: PROCESSED: HELLO WORLD

Logs:
[ENRICHED] ... [PROCESSED: HELLO WORLD]
```

---

### 2️⃣ Intermediate Patterns (Pub/Sub & Routing)

- **Content-Based Routing (JSON, XML, CSV)**
- **Splitter Pattern (CSV)**
- **Pub/Sub with Multiple Subscribers**

```bash
# JSON Order (routes to orderEventChannel - 4 subscribers)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId": "12345", "customer": "John Doe", "amount": 299.99}'

# XML Inventory (routes to inventoryEventChannel - 3 subscribers)
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/xml" \
  -d '<inventory><item>Laptop</item><quantity>50</quantity></inventory>'

# CSV Batch (splits and routes to inventoryEventChannel)
curl -X POST http://localhost:8080/api/batch \
  -H "Content-Type: text/plain" \
  -d "item1,item2,item3"
```
**Expected Output:**
```
JSON Order Response: Order processed: {"orderId": "12345", ...}

Logs:
[ORDER-SERVICE] received: {"orderId": "12345", ...}
[AUDIT-SERVICE] received: {"orderId": "12345", ...}
[EMAIL-SERVICE] received: {"orderId": "12345", ...}
[ANALYTICS-SERVICE] received: {"orderId": "12345", ...}

---

XML Inventory Response: Inventory processed: <inventory>...

Logs:
[INVENTORY-SERVICE] received: <inventory>...
[WAREHOUSE-SERVICE] received: <inventory>...
[AUDIT-SERVICE] received: <inventory>...

---

CSV Batch Response: Inventory processed: ITEM: item3

Logs:
[CSV-ITEM-PROCESSOR] received: item1
[CSV-ITEM-PROCESSOR] received: item2
[CSV-ITEM-PROCESSOR] received: item3
[INVENTORY-SERVICE] received: ITEM: item1
[WAREHOUSE-SERVICE] received: ITEM: item1
[AUDIT-SERVICE] received: ITEM: item1
[INVENTORY-SERVICE] received: ITEM: item2
...
```

---

### 3️⃣ Advanced Patterns

#### 🔹 Message Aggregator Pattern

Groups messages by `correlationId` and releases them when:
- 3 or more messages collected, OR
- 10 seconds timeout expires

**Test 1: Immediate Release (3 messages)**
```bash
# Send 3 messages with same correlationId (in separate terminals or quickly)
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -d "Part 1"

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -d "Part 2"

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -d "Part 3"
```
**Expected Output:**
```
Response (after 3rd message): Aggregated: [Part 1,Part 3,Part 2]

Logs:
[AGGREGATION-PROCESSOR] received: Part 1,Part 3,Part 2
```

**Test 2: Timeout Release (partial aggregation)**
```bash
# Send only 2 messages - wait 10+ seconds for timeout
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-456" \
  -d "Part A"

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-456" \
  -d "Part B"

# Wait 10 seconds...
```
**Expected Output:**
```
Response (after 10s timeout): Aggregated: [Part A,Part B]

Logs:
[AGGREGATION-PROCESSOR] received: Part A,Part B
```

**Test 3: Multiple Correlation Groups**
```bash
# Different correlationIds aggregate independently
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" -d "Item 1" &
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" -d "Item 2" &
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" -d "Item 3" &

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" -d "Widget A" &
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" -d "Widget B" &
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" -d "Widget C" &

wait
```
**Expected Output:**
```
Response: Aggregated: [Item 1,Item 2,Item 3]
Response: Aggregated: [Widget A,Widget B,Widget C]

Logs:
[AGGREGATION-PROCESSOR] received: Item 1,Item 2,Item 3
[AGGREGATION-PROCESSOR] received: Widget A,Widget B,Widget C
```

---

#### 🔹 Scatter-Gather Pattern

Broadcasts message to multiple processors and gathers results.

```bash
curl -X POST http://localhost:8080/api/advanced/scatter-gather \
  -H "Content-Type: text/plain" \
  -d "Process this in parallel"
```
**Expected Output:**
```
Response: Scatter-Gather processed: Processed by scatter: Process this in parallel

Logs:
[SCATTER-GATHER] scatter: Process this in parallel
[SCATTER-GATHER] gather: Processed by scatter: Process this in parallel
```

---

#### 🔹 File Integration Pattern

Automatically processes files from input directory.

```bash
# Create input directory if not exists
mkdir -p /tmp/input /tmp/output

# Create test file
echo "This is test content for file processing" > /tmp/input/test-file.txt

# Wait 5 seconds (poller interval)
sleep 6

# Check output
ls -la /tmp/output/
cat /tmp/output/processed-*.txt
```
**Expected Output:**
```
Logs:
[FILE-PROCESSOR] received: /tmp/input/test-file.txt

Output File Content:
PROCESSED: /tmp/input/test-file.txt

Output Files:
/tmp/output/processed-1727634840123.txt
```

---

#### 🔹 Retry Pattern

Simulates retrying failed operations.

```bash
curl -X POST http://localhost:8080/api/advanced/retry \
  -H "Content-Type: text/plain" \
  -d "This might fail"
```
**Expected Output:**
```
Response: Retry processed: This might fail

Logs:
[FLAKY-SERVICE] received: This might fail
```

---

#### 🔹 Control Bus Pattern

Dynamically control Spring Integration components at runtime.

```bash
# Stop a component
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: text/plain" \
  -d "@fileInputAdapter.stop()"

# Start a component
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: text/plain" \
  -d "@fileInputAdapter.start()"

# Check if running
curl -X POST http://localhost:8080/api/control \
  -H "Content-Type: text/plain" \
  -d "@fileInputAdapter.isRunning()"
```
**Expected Output:**
```
Response: true (or false, depending on component state)

Logs:
Component state changed based on command
```

---

## 🔍 Monitoring & Debugging

- Check logs for `[SERVICE] received: ...` lines.
- All message headers include enrichment info.
- All patterns are available in a single XML for easy testing.
- Use `tail -f application.log` to watch real-time processing.

---

## 🎓 Learning Path

1. **Start with Basic** (enricher, filter, transformer)
   - Understand message flow and transformation
2. **Try Intermediate** (routing, pub/sub, splitter)
   - Learn content-based routing and pub/sub patterns
3. **Explore Advanced** (aggregation, scatter-gather, file, retry, control bus)
   - Master complex integration patterns

---

## 🐛 Troubleshooting

### Common Issues:

1. **Aggregator not working**: 
   - Ensure `correlationId` header is lowercase in configuration
   - Check that messages have the same correlationId

2. **File processing not working**:
   - Ensure `/tmp/input/` and `/tmp/output/` exist
   - Check file permissions
   - Wait 5+ seconds for poller to pick up files

3. **Messages not appearing**:
   - Check application logs
   - Verify endpoint URLs are correct
   - Ensure Spring Integration context loaded successfully

4. **Health check**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## 📊 Pattern Summary

| Pattern | Endpoint | Key Feature |
|---------|----------|-------------|
| Basic Transform | `/api/basic-transform` | Enrichment + Filter + Transform |
| Order Routing | `/api/orders` | JSON → 4 subscribers |
| Inventory Routing | `/api/inventory` | XML → 3 subscribers |
| Batch Processing | `/api/batch` | CSV → Split → Route |
| Aggregator | `/api/advanced/aggregate` | Group by correlationId |
| Scatter-Gather | `/api/advanced/scatter-gather` | Parallel processing |
| File Processing | File system | Auto-process files |
| Retry | `/api/advanced/retry` | Fault tolerance |
| Control Bus | `/api/control` | Runtime control |

---

This progressive setup lets you learn EIP patterns step by step! 🚀