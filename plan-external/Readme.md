
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
[ENRICHED] ... [PROCESSED: HELLO WORLD]
```

---

### 2️⃣ Intermediate Patterns (Pub/Sub & Routing)

- **Content-Based Routing (JSON, XML, CSV)**
- **Splitter Pattern (CSV)**
- **Pub/Sub with Multiple Subscribers**

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId": "12345", "customer": "John Doe", "amount": 299.99}'

curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/xml" \
  -d '<inventory><item>Laptop</item><quantity>50</quantity></inventory>'

curl -X POST http://localhost:8080/api/batch \
  -H "Content-Type: text/plain" \
  -d "item1,item2,item3"
```
**Expected Output:**
```
[ORDER-SERVICE] received: ...
[INVENTORY-SERVICE] received: ...
[CSV-ITEM-PROCESSOR] received: item1
...
```

---

### 3️⃣ Advanced Patterns

- **Message Aggregator**
- **Scatter-Gather**
- **File Integration**
- **Retry Pattern**
- **Control Bus**

```bash
# Aggregator (must include correlationId header exactly as shown)
curl -X POST http://localhost:8080/api/advanced/aggregate -H "correlationId: ORDER-123" -d "Part 1"
curl -X POST http://localhost:8080/api/advanced/aggregate -H "correlationId: ORDER-123" -d "Part 2"
curl -X POST http://localhost:8080/api/advanced/aggregate -H "correlationId: ORDER-123" -d "Part 3"

# Scatter-Gather
curl -X POST http://localhost:8080/api/advanced/scatter-gather -d "Process this in parallel"

# File Integration
echo "File content" > /tmp/input/test-file.txt
ls /tmp/output/

# Retry
curl -X POST http://localhost:8080/api/advanced/retry -d "This might fail"

# Control Bus
curl -X POST http://localhost:8080/api/control -d "@controlBus.stop()"
```

---

## 🔍 Monitoring & Debugging

- Check logs for `[SERVICE] received: ...` lines.
- All message headers include enrichment info.
- All patterns are available in a single XML for easy testing.

---

## 🎓 Learning Path

1. Start with Basic (enricher, filter, transformer)
2. Try Intermediate (routing, pub/sub, splitter)
3. Explore Advanced (aggregation, scatter-gather, file, retry, control bus)

---

## 🐛 Troubleshooting

- Ensure `/tmp/input/` and `/tmp/output/` exist.
- All handlers use `SimpleHandler` (no Java changes needed).
- Use `curl http://localhost:8080/actuator/health` for health check.

---

This progressive setup lets you learn EIP patterns step by step! 🚀