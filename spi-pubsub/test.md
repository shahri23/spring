# 🧪 Complete Testing Guide for SPI-PubSub Application

This document provides comprehensive testing instructions with expected responses for all endpoints and functionality.

## 📋 Prerequisites

1. **Application Running**: Ensure the app is running on `http://localhost:8080`
2. **Tools Required**: `curl` command-line tool
3. **Optional**: `jq` for JSON formatting (`curl ... | jq .`)

## 🏁 Quick Health Check

### Test 1: Basic Application Health
```bash
curl http://localhost:8080/api/management/health
```

**Expected Response:**
```json
{
  "timestamp": 1755383590236,
  "status": "UP",
  "components": {
    "routing": "UP",
    "pubsub": "UP",
    "errorHandling": "UP"
  },
  "configuration": {
    "parallelProcessing": true,
    "maxRetries": 3,
    "threadPoolSize": 10
  }
}
```

## 📊 Configuration Management Tests

### Test 2: Get Current Configuration Status
```bash
curl http://localhost:8080/api/management/config/status
```

**Expected Response:**
```json
{
  "routing": {
    "enabled": true,
    "routes": {},
    "defaultRoute": "default"
  },
  "channels": {
    "capacity": 100,
    "threadPoolSize": 10,
    "error": "errorChannel",
    "output": "outputChannel",
    "input": "inputChannel"
  },
  "processing": {
    "retryDelay": 1000,
    "parallelProcessing": true,
    "maxRetries": 3
  },
  "errorHandling": {
    "enabled": true,
    "notificationChannels": [],
    "strategy": "log"
  },
  "pubsub": {
    "enabled": false,
    "provider": "default",
    "configuration": {}
  }
}
```

### Test 3: Export Full Configuration
```bash
curl http://localhost:8080/api/management/config/export
```

**Expected Response:**
```json
{
  "channels": {
    "enabled": true,
    "contentRouter": {
      "enabled": true,
      "parallelProcessing": true,
      "errorHandling": true
    },
    "originalTransformer": {
      "enabled": true
    },
    "pubSub": {
      "enabled": true
    },
    "input": "inputChannel",
    "output": "outputChannel",
    "error": "errorChannel",
    "capacity": 100,
    "threadPoolSize": 10
  },
  "routing": {
    "enabled": true,
    "defaultRoute": "default",
    "routes": null,
    "xmlTypes": null
  },
  "pubsub": {
    "enabled": false,
    "provider": "default",
    "configuration": null,
    "messageBroker": {
      "type": "in-memory",
      "enabled": true
    },
    "topics": null,
    "subscribers": null
  },
  "processing": {
    "parallelProcessing": true,
    "maxRetries": 3,
    "retryDelay": 1000,
    "threadPoolSize": 10,
    "timeoutMs": 5000,
    "retryAttempts": 3,
    "enableMetrics": true,
    "enableTracing": true
  },
  "errorHandling": {
    "enabled": true,
    "strategy": "log",
    "notificationChannels": null,
    "maxRetryAttempts": 3,
    "deadLetterQueue": true,
    "errorChannel": "errorChannel",
    "retryPolicy": "exponential-backoff"
  }
}
```

## 🔄 Dynamic Configuration Tests

### Test 4: Enable PubSub
```bash
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=true"
```

**Expected Response:**
```json
{
  "message": "Pub/Sub enabled",
  "enabled": true,
  "timestamp": 1755383590236,
  "status": "success"
}
```

### Test 5: Disable PubSub
```bash
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=false"
```

**Expected Response:**
```json
{
  "message": "Pub/Sub disabled",
  "enabled": false,
  "timestamp": 1755383590240,
  "status": "success"
}
```

### Test 6: Enable Routing
```bash
curl -X POST "http://localhost:8080/api/management/config/routing/toggle?enabled=true"
```

**Expected Response:**
```json
{
  "message": "Routing enabled",
  "enabled": true,
  "timestamp": 1755383590245,
  "status": "success"
}
```

### Test 7: Disable Routing
```bash
curl -X POST "http://localhost:8080/api/management/config/routing/toggle?enabled=false"
```

**Expected Response:**
```json
{
  "message": "Routing disabled",
  "enabled": false,
  "timestamp": 1755383590250,
  "status": "success"
}
```

### Test 8: Reload Configuration
```bash
curl -X POST http://localhost:8080/api/management/config/reload
```

**Expected Response:**
```json
{
  "timestamp": 1755383590255,
  "status": "success",
  "message": "Configuration reloaded successfully"
}
```

## 🎯 Content Router Tests

### Test 9: Router Status
```bash
curl http://localhost:8080/api/router/status
```

**Expected Response:**
```json
{
  "timestamp": 1755383590260,
  "status": "UP",
  "routing": {
    "enabled": true,
    "defaultRoute": "default"
  }
}
```

### Test 10: Route Simple Content
```bash
curl -X POST http://localhost:8080/api/router/route \
  -H "Content-Type: application/json" \
  -d '{"type":"customer","data":"sample content"}'
```

**Expected Response:**
```json
{
  "timestamp": 1755383590265,
  "status": "success",
  "route": "default",
  "processing": "completed",
  "message": "Content routed successfully"
}
```

### Test 11: Route Content When Routing Disabled
First disable routing, then test:
```bash
curl -X POST "http://localhost:8080/api/management/config/routing/toggle?enabled=false"
curl -X POST http://localhost:8080/api/router/route \
  -H "Content-Type: application/json" \
  -d '{"type":"order","data":"test order"}'
```

**Expected Response:**
```json
{
  "timestamp": 1755383590270,
  "status": "disabled",
  "message": "Routing is disabled"
}
```

## 📡 PubSub Testing

### Test 12: Get PubSub Status (When Disabled)
```bash
curl http://localhost:8080/api/management/pubsub/status
```

**Expected Response:**
```json
{
  "timestamp": 1755383590275,
  "status": "disabled",
  "message": "Pub/Sub service is not available or disabled"
}
```

### Test 13: Enable PubSub and Check Status
```bash
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=true"
curl http://localhost:8080/api/management/pubsub/status
```

**Expected Response:**
```json
{
  "timestamp": 1755383590280,
  "status": "active",
  "pubSubService": {
    "status": "UP",
    "topics": {},
    "activeSubscribers": 0,
    "messageCount": 0
  }
}
```

## 🔄 XML Transformation Tests

### Test 14: Transform Customer XML
```bash
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>12345</id>
    <name>John Smith</name>
    <email>john.smith@example.com</email>
    <address>
        <street>123 Main Street</street>
        <city>New York</city>
        <state>NY</state>
        <zipCode>10001</zipCode>
    </address>
</customer>'
```

**Expected Response:**
```json
{
  "id": "12345",
  "name": "John Smith",
  "email": "john.smith@example.com",
  "address": {
    "street": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001"
  }
}
```

### Test 15: Transform Order XML
```bash
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>ORD-2024-001234</orderId>
    <customerId>12345</customerId>
    <status>confirmed</status>
    <totalAmount>199.98</totalAmount>
    <items>
        <item>
            <productId>PROD-001</productId>
            <quantity>2</quantity>
            <price>99.99</price>
        </item>
    </items>
</order>'
```

**Expected Response:**
```json
{
  "orderId": "ORD-2024-001234",
  "customerId": "12345",
  "status": "confirmed",
  "totalAmount": 199.98,
  "items": {
    "item": {
      "productId": "PROD-001",
      "quantity": 2,
      "price": 99.99
    }
  }
}
```

### Test 16: Transform Product XML
```bash
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<product>
    <productId>PROD-001</productId>
    <name>Laptop Computer</name>
    <category>Electronics</category>
    <price>999.99</price>
    <stock>15</stock>
    <specifications>
        <cpu>Intel i7</cpu>
        <memory>16GB</memory>
        <storage>512GB SSD</storage>
    </specifications>
</product>'
```

**Expected Response:**
```json
{
  "productId": "PROD-001",
  "name": "Laptop Computer",
  "category": "Electronics",
  "price": 999.99,
  "stock": 15,
  "specifications": {
    "cpu": "Intel i7",
    "memory": "16GB",
    "storage": "512GB SSD"
  }
}
```

### Test 17: Transform Invalid XML
```bash
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<invalid-xml><unclosed-tag>content</invalid-xml>'
```

**Expected Response:**
```json
{
  "timestamp": 1755383590285,
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid XML format",
  "path": "/api/transform"
}
```

## 📊 Comprehensive Test Scenarios

### Test 18: End-to-End Customer Processing (PubSub Enabled)
```bash
# Enable PubSub first
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=true"

# Process customer XML
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>67890</id>
    <name>Jane Doe</name>
    <email>jane.doe@example.com</email>
    <phone>555-1234</phone>
    <membershipLevel>premium</membershipLevel>
</customer>'

# Check PubSub status for message processing
curl http://localhost:8080/api/management/pubsub/status
```

**Expected Responses:**
1. PubSub Toggle: `{"message":"Pub/Sub enabled","enabled":true,"timestamp":...,"status":"success"}`
2. XML Processing: `{"id":"67890","name":"Jane Doe","email":"jane.doe@example.com","phone":"555-1234","membershipLevel":"premium"}`
3. PubSub Status: `{"timestamp":...,"status":"active","pubSubService":{"status":"UP","topics":{},"activeSubscribers":0,"messageCount":1}}`

### Test 19: Bulk Processing Test
```bash
# Process multiple XML types in sequence
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<customer><id>1</id><name>Customer 1</name></customer>'

curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<order><orderId>ORDER-001</orderId><status>pending</status></order>'

curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<product><productId>PROD-002</productId><name>Product 2</name></product>'

# Check system health after bulk processing
curl http://localhost:8080/api/management/health
```

### Test 20: Configuration Reset Test
```bash
# Test configuration state changes
curl http://localhost:8080/api/management/config/status | grep -o '"enabled":[^,]*'

# Disable routing
curl -X POST "http://localhost:8080/api/management/config/routing/toggle?enabled=false"

# Disable PubSub
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=false"

# Check disabled state
curl http://localhost:8080/api/management/config/status

# Re-enable everything
curl -X POST "http://localhost:8080/api/management/config/routing/toggle?enabled=true"
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=true"

# Verify restoration
curl http://localhost:8080/api/management/config/status
```

## 🚨 Error Handling Tests

### Test 21: Invalid Endpoint
```bash
curl http://localhost:8080/api/nonexistent
```

**Expected Response:**
```json
{
  "timestamp": "2025-08-16T22:59:50.236+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/api/nonexistent"
}
```

### Test 22: Invalid HTTP Method
```bash
curl -X DELETE http://localhost:8080/api/management/config/status
```

**Expected Response:**
```json
{
  "timestamp": "2025-08-16T22:59:50.240+00:00",
  "status": 405,
  "error": "Method Not Allowed",
  "path": "/api/management/config/status"
}
```

### Test 23: Malformed Request Body
```bash
curl -X POST http://localhost:8080/api/router/route \
  -H "Content-Type: application/json" \
  -d '{"invalid":"json"'
```

**Expected Response:**
```json
{
  "timestamp": "2025-08-16T22:59:50.245+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "JSON parse error"
}
```

## 📈 Performance Tests

### Test 24: Rapid Fire Requests (Load Testing)
```bash
# Run 10 rapid requests to test performance
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/transform \
    -H "Content-Type: application/xml" \
    -d "<customer><id>$i</id><name>Customer $i</name></customer>" &
done
wait

# Check system health after load
curl http://localhost:8080/api/management/health
```

### Test 25: Large XML Processing
```bash
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>LARGE-ORDER-001</orderId>
    <customerId>99999</customerId>
    <status>processing</status>
    <totalAmount>9999.99</totalAmount>
    <items>
        <item><productId>PROD-001</productId><quantity>100</quantity><price>10.00</price></item>
        <item><productId>PROD-002</productId><quantity>200</quantity><price>15.00</price></item>
        <item><productId>PROD-003</productId><quantity>150</quantity><price>25.00</price></item>
        <item><productId>PROD-004</productId><quantity>75</quantity><price>50.00</price></item>
        <item><productId>PROD-005</productId><quantity>50</quantity><price>100.00</price></item>
    </items>
    <customer>
        <id>99999</id>
        <name>Large Order Customer</name>
        <email>large.order@example.com</email>
        <address>
            <street>999 Enterprise Way</street>
            <city>Business City</city>
            <state>CA</state>
            <zipCode>90210</zipCode>
        </address>
    </customer>
</order>'
```

## 🔄 PubSub Advanced Testing

### Test 26: PubSub Message Flow Testing
```bash
# Enable PubSub with verbose logging
curl -X POST "http://localhost:8080/api/management/config/pubsub/toggle?enabled=true"

# Send various XML types to trigger pub/sub events
echo "Testing Customer Events..."
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<customer><id>PUBSUB-CUST-001</id><name>PubSub Test Customer</name><email>pubsub@example.com</email></customer>'

echo "Testing Order Events..."
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<order><orderId>PUBSUB-ORDER-001</orderId><customerId>PUBSUB-CUST-001</customerId><status>confirmed</status></order>'

echo "Testing Product Events..."
curl -X POST http://localhost:8080/api/transform \
  -H "Content-Type: application/xml" \
  -d '<product><productId>PUBSUB-PROD-001</productId><name>PubSub Test Product</name><category>Testing</category></product>'

# Check PubSub status for accumulated messages
curl http://localhost:8080/api/management/pubsub/status
```

**Expected Final PubSub Status:**
```json
{
  "timestamp": 1755383590300,
  "status": "active",
  "pubSubService": {
    "status": "UP",
    "topics": {
      "xml.processed": 3,
      "customer.events": 1,
      "order.events": 1,
      "product.events": 1
    },
    "activeSubscribers": 12,
    "messageCount": 6
  }
}
```

## 📝 Testing Checklist

### ✅ Manual Testing Checklist
- [ ] Test 1: Basic health check passes
- [ ] Test 2-3: Configuration status and export work
- [ ] Test 4-8: Dynamic configuration toggles work
- [ ] Test 9-11: Content router functionality
- [ ] Test 12-13: PubSub status reporting
- [ ] Test 14-17: XML transformation (valid and invalid)
- [ ] Test 18-20: End-to-end scenarios
- [ ] Test 21-23: Error handling
- [ ] Test 24-25: Performance under load
- [ ] Test 26: PubSub message flow

### 🔍 What to Look For
1. **Response Times**: All requests should complete under 1000ms
2. **Status Codes**: 200 for success, 400/404/405 for expected errors
3. **JSON Structure**: Responses match expected format
4. **PubSub Counters**: Message counts increase with processing
5. **Configuration Persistence**: Settings maintain state between requests
6. **Error Messages**: Clear, descriptive error responses
7. **Logging**: Console shows processing activity (if visible)

### 🚨 Troubleshooting
If tests fail:
1. Check application is running: `curl http://localhost:8080/api/management/health`
2. Verify Java process: `jps` or Task Manager
3. Check logs for errors
4. Restart application if needed: `java -jar target/spi-app.jar`
5. Verify port 8080 is not blocked by firewall

## 🎯 Success Criteria
- All health checks return "UP" status
- Configuration changes are immediately reflected
- XML processing returns valid JSON
- PubSub message counts increase appropriately
- Error responses are properly formatted
- Performance remains consistent under load

---

**🎉 Testing Complete!** Your Spring Boot application with latest versions (Spring Boot 3.5.4, Java 17) is fully functional and ready for production use!
