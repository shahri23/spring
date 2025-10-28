# Enterprise Integration Patterns - Test Guide

## PART 1: Basic Pipeline Patterns

### 1. Content Enricher
- **Pattern**: Adds metadata headers (timestamp, correlationId, source, stage) to messages
- **Test**: 
  ```bash
  curl -X POST http://localhost:8080/api/basic-transform \
    -H "Content-Type: text/plain" \
    -d "hello world"
  ```
- **Expected Output**: `[BASIC-PROCESSOR] received: HELLO WORLD`

### 2. Message Filter
- **Pattern**: Validates messages are non-null and non-empty
- **Test**: 
  ```bash
  curl -X POST http://localhost:8080/api/basic-transform \
    -H "Content-Type: text/plain" \
    -d ""
  ```
- **Expected Output**: Message rejected, logged to error channel

### 3. Message Translator/Transformer
- **Pattern**: Transforms payload to uppercase with prefix
- **Test**: Same as Content Enricher (#1)
- **Expected Output**: Transformed output with uppercase text

### 4. Publish-Subscribe Channel
- **Pattern**: Broadcasts processed messages to multiple subscribers
- **Test**: Any successful basic-transform call
- **Expected Output**: Multiple log entries from different subscribers

---

## PART 2: Routing & Distribution Patterns

### 5. Content-Based Router
- **Pattern**: Routes messages based on content format (JSON/XML/CSV/default)
- **Test (JSON)**: 
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"order":"123"}'
  ```
- **Expected Output**: `Order processed: {"order":"123"}` + logs from 4 services (ORDER, AUDIT, EMAIL, ANALYTICS)

### 6. Splitter
- **Pattern**: Splits CSV into individual items
- **Test**: 
  ```bash
  curl -X POST http://localhost:8080/api/batch \
    -H "Content-Type: text/plain" \
    -d "apple,banana,orange"
  ```
- **Expected Output**: 3 separate items processed individually

### 7. Aggregator
- **Pattern**: Collects split messages back into single response
- **Test**: Same as Splitter (#6)
- **Expected Output**: `Batch processed 3 items: [ITEM: apple, ITEM: banana, ITEM: orange]`

### 8. Publish-Subscribe with Multiple Subscribers
- **Pattern**: CSV items broadcast to 5 subscribers (logger, inventory, warehouse, audit, aggregator)
- **Test**: Same as Splitter (#6)
- **Expected Output**: 
  - 5 log entries per CSV item
  - Final aggregated reply with all items

### 9. Message Channel (Various Types)
- **Pattern**: Point-to-point and pub-sub channels throughout the pipeline
- **Test**: All endpoints demonstrate channel usage
- **Expected Output**: Proper message flow through pipeline stages

### 10. Competing Consumers (Implicit)
- **Pattern**: Async task executor with thread pool for parallel processing
- **Test**: Multiple concurrent requests to any endpoint
- **Expected Output**: Parallel processing with thread pool (2-5 threads)

---

## Additional Test Commands

### XML Routing Test
```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/xml" \
  -d '<inventory><item>widget</item></inventory>'
```
- **Expected Output**: `Inventory processed: <inventory>...` + 3 service logs (INVENTORY, WAREHOUSE, AUDIT)

### Invalid Message Test
```bash
curl -X POST http://localhost:8080/api/basic-transform \
  -H "Content-Type: text/plain" \
  -d "   "
```
- **Expected Output**: Message filtered out, error log entry

### Default Route Test
```bash
curl -X POST http://localhost:8080/api/basic-transform \
  -H "Content-Type: text/plain" \
  -d "plain text message"
```
- **Expected Output**: `[DEFAULT-HANDLER] received: plain text message`

---

## Summary of Patterns Demonstrated

1. **Content Enricher** - Metadata enhancement
2. **Message Filter** - Validation and rejection
3. **Message Translator** - Payload transformation
4. **Publish-Subscribe Channel** - Event broadcasting
5. **Content-Based Router** - Format-based routing
6. **Splitter** - Message decomposition
7. **Aggregator** - Message recomposition
8. **Competing Consumers** - Parallel processing
9. **Message Channel** - Communication infrastructure
10. **Service Activator** - Business logic invocation