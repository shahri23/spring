# 🚀 EIP Demo Testing Guide

## 📁 File Structure Setup

Place these files in your system:

```
/tmp/
├── eip-basic-transforms.xml      # File 1: Basic transformations
├── eip-routing-pubsub.xml        # File 2: Routing & Pub/Sub
└── eip-advanced-patterns.xml     # File 3: Advanced enterprise patterns

/tmp/input/                       # Create this directory for file processing
/tmp/output/                      # Create this directory for file output
```

## 🎯 Profile-Based Testing

### Test Only Basic Patterns
```bash
export SPRING_PROFILES_ACTIVE=basic
mvn spring-boot:run
```

### Test Basic + Routing Patterns  
```bash
export SPRING_PROFILES_ACTIVE=intermediate
mvn spring-boot:run
```

### Test All Patterns (Default)
```bash
export SPRING_PROFILES_ACTIVE=demo
mvn spring-boot:run
```

---

## 🧪 Testing Each EIP Pattern

### FILE 1: Basic Transformation Patterns

#### 1. **Message Enricher + Filter + Transformer**
```bash
# Valid message (will be processed)
curl -X POST http://localhost:8080/api/basic-transform \
  -H "Content-Type: text/plain" \
  -d "Hello World"

# Invalid message (will be filtered out)
curl -X POST http://localhost:8080/api/basic-transform \
  -H "Content-Type: text/plain" \
  -d ""
```

**Expected Output:**
```
[BASIC-MONITOR] received: Hello World
[BASIC-MESSAGE-PROCESSOR] received: HELLO WORLD  
[FINAL-OUTPUT-PROCESSOR] received: HELLO WORLD
```

---

### FILE 2: Routing & Pub/Sub Patterns

#### 2. **Content-Based Routing - JSON Route**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId": "12345", "customer": "John Doe", "amount": 299.99}'
```

**Expected Output:**
```
[ORDER-SERVICE] received: {"orderId": "12345"...}
[AUDIT-SERVICE] received: {"orderId": "12345"...}
[EMAIL-NOTIFICATION-SERVICE] received: {"orderId": "12345"...}
[ANALYTICS-SERVICE] received: {"orderId": "12345"...}
```

#### 3. **Content-Based Routing - XML Route**
```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/xml" \
  -d '<inventory><item>Laptop</item><quantity>50</quantity></inventory>'
```

**Expected Output:**
```
[INVENTORY-SERVICE] received: <inventory>...
[WAREHOUSE-SERVICE] received: <inventory>...
[AUDIT-SERVICE] received: <inventory>...
```

#### 4. **Splitter Pattern - CSV Route**
```bash
curl -X POST http://localhost:8080/api/batch \
  -H "Content-Type: text/plain" \
  -d "item1,item2,item3,item4,item5"
```

**Expected Output:**
```
[CSV-ITEM-PROCESSOR] received: item1
[CSV-ITEM-PROCESSOR] received: item2
[INVENTORY-SERVICE] received: ITEM: item1
[WAREHOUSE-SERVICE] received: ITEM: item1
[INVENTORY-SERVICE] received: ITEM: item2
...
```

#### 5. **Pub/Sub with Multiple Subscribers**
Each message triggers multiple independent services showing loose coupling!

---

### FILE 3: Advanced Enterprise Patterns

#### 6. **Message Aggregator Pattern**
```bash
# Send 3 messages with same correlation ID to trigger aggregation
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
[AGGREGATION-PROCESSOR] received: [Part 1, Part 2, Part 3]
```

#### 7. **Scatter-Gather Pattern**
```bash
curl -X POST http://localhost:8080/api/advanced/scatter-gather \
  -H "Content-Type: text/plain" \
  -d "Process this in parallel"
```

**Expected Output:**
```
[PROCESSOR-A] received: Process this in parallel
[PROCESSOR-B] received: Process this in parallel  
[PROCESSOR-C] received: Process this in parallel
# Results aggregated together
```

#### 8. **File Integration Pattern**
```bash
# Create test file
echo "File content to process" > /tmp/input/test-file.txt

# Wait 5 seconds for polling, then check output
ls /tmp/output/
```

**Expected Output:**
```
[FILE-PROCESSOR] received: File [/tmp/input/test-file.txt]
# Processed file appears in /tmp/output/
```

#### 9. **Retry Pattern**
```bash
curl -X POST http://localhost:8080/api/advanced/retry \
  -d "This might fail"
```

**Expected Output:**
```
[FLAKY-SERVICE] received: This might fail
# If it fails, automatic retry with exponential backoff
```

#### 10. **Control Bus Pattern**
```bash
# Stop a specific component
curl -X POST http://localhost:8080/api/control \
  -d "@controlBus.stop()"

# Start a component  
curl -X POST http://localhost:8080/api/control \
  -d "@controlBus.start()"
```

---

## 🔍 Monitoring & Debugging

### Check Application Logs
```bash
tail -f logs/spring.log
```

### Look for These Log Patterns:
- **Enrichment**: Messages show added headers (timestamp, correlationId)
- **Routing**: Messages route to different handlers based on content
- **Pub/Sub**: Same message processed by multiple services
- **Aggregation**: Multiple messages combined into one
- **File Processing**: Files moved from input to output directories

### Verify Message Headers
Each processed message should show:
```
Headers: {
  timestamp=1694353200000,
  correlationId=a1b2c3d4-e5f6-7890-abcd-ef1234567890,
  source=EIP-BASIC-DEMO,
  messageSize=50,
  stage=ENRICHED
}
```

---

## 🎓 Learning Path Recommendation

1. **Start with Basic** (`SPRING_PROFILES_ACTIVE=basic`)
   - Understand message flow
   - See enrichment and filtering in action

2. **Add Routing** (`SPRING_PROFILES_ACTIVE=intermediate`)  
   - Test different content types
   - Observe pub/sub fan-out

3. **Full Enterprise** (`SPRING_PROFILES_ACTIVE=demo`)
   - Test advanced patterns
   - See aggregation and file processing

---

## 🐛 Troubleshooting

### Common Issues:
1. **Directories don't exist**: Create `/tmp/input/` and `/tmp/output/`
2. **Port conflicts**: Change port in `application.yml`
3. **Handler classes missing**: All handlers use `SimpleHandler` - should work out of the box

### Quick Health Check:
```bash
curl http://localhost:8080/actuator/health
```

This progressive setup lets you learn EIP patterns step by step! 🚀