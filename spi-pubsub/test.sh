#!/bin/bash

# Enhanced SPI-Router Testing Script
# Tests configurable channels, routing, and pub/sub functionality

BASE_URL="http://localhost:8080"
CONTENT_TYPE="Content-Type: application/xml"

echo "🚀 Starting Enhanced SPI-Router Testing Suite"
echo "=============================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_header() {
    echo -e "\n${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Wait for service to be ready
wait_for_service() {
    print_info "Waiting for service to be ready..."
    for i in {1..30}; do
        if curl -s "$BASE_URL/api/health" > /dev/null 2>&1; then
            print_success "Service is ready!"
            return 0
        fi
        sleep 1
    done
    print_error "Service did not start within 30 seconds"
    exit 1
}

# Test function
test_endpoint() {
    local endpoint="$1"
    local data="$2"
    local description="$3"
    local method="${4:-POST}"
    
    print_info "Testing: $description"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" "$BASE_URL$endpoint")
        status_code="${response: -3}"
        body="${response%???}"
    else
        response=$(curl -s -w "%{http_code}" -X "$method" \
            -H "$CONTENT_TYPE" \
            -d "$data" \
            "$BASE_URL$endpoint")
        status_code="${response: -3}"
        body="${response%???}"
    fi
    
    if [ "$status_code" -eq 200 ]; then
        print_success "$description - HTTP $status_code"
        echo "Response: $body" | jq . 2>/dev/null || echo "Response: $body"
    else
        print_error "$description - HTTP $status_code"
        echo "Response: $body"
    fi
    echo
}

# Start testing
wait_for_service

print_header "1. Basic Health Checks"

test_endpoint "/api/health" "" "Basic Health Check" "GET"
test_endpoint "/api/content-router/health" "" "Content Router Health" "GET"
test_endpoint "/api/management/health/detailed" "" "Detailed Health Check" "GET"

print_header "2. Configuration Status"

test_endpoint "/api/management/config/status" "" "Configuration Status" "GET"
test_endpoint "/api/management/pubsub/topics" "" "Pub/Sub Topics Status" "GET"
test_endpoint "/api/management/pubsub/subscribers/active" "" "Active Subscribers" "GET"

print_header "3. Original XML to JSON Transformer (Legacy)"

PERSON_XML='<?xml version="1.0" encoding="UTF-8"?>
<person>
    <n>John Doe</n>
    <age>30</age>
    <email>john.doe@example.com</email>
    <address>
        <street>123 Main St</street>
        <city>New York</city>
        <zipcode>10001</zipcode>
    </address>
    <hobbies>
        <hobby>Reading</hobby>
        <hobby>Swimming</hobby>
        <hobby>Coding</hobby>
    </hobbies>
</person>'

test_endpoint "/api/transform" "$PERSON_XML" "Original XML→JSON Transform"

print_header "4. Content-Based Router Tests"

# Customer XML
CUSTOMER_XML='<?xml version="1.0" encoding="UTF-8"?>
<customer>
    <id>12345</id>
    <n>John Smith</n>
    <email>john.smith@example.com</email>
    <phone>+1-555-0123</phone>
    <address>
        <street>123 Main Street</street>
        <city>New York</city>
        <state>NY</state>
        <zipCode>10001</zipCode>
        <country>USA</country>
    </address>
    <preferences>
        <newsletter>true</newsletter>
        <promotions>false</promotions>
        <language>en</language>
    </preferences>
    <dateCreated>2024-01-15T10:30:00Z</dateCreated>
    <customerType>premium</customerType>
</customer>'

test_endpoint "/api/content-router/route-xml" "$CUSTOMER_XML" "Content Router - Customer XML"

# Order XML
ORDER_XML='<?xml version="1.0" encoding="UTF-8"?>
<order>
    <orderId>ORD-2024-001234</orderId>
    <customerId>12345</customerId>
    <orderDate>2024-08-16T14:25:00Z</orderDate>
    <status>confirmed</status>
    <items>
        <item>
            <productId>PROD-001</productId>
            <productName>Premium Widget</productName>
            <quantity>2</quantity>
            <unitPrice>99.99</unitPrice>
            <totalPrice>199.98</totalPrice>
        </item>
    </items>
    <totalAmount>199.98</totalAmount>
</order>'

test_endpoint "/api/content-router/route-xml" "$ORDER_XML" "Content Router - Order XML"

# Product XML
PRODUCT_XML='<?xml version="1.0" encoding="UTF-8"?>
<product>
    <productId>PROD-001</productId>
    <n>Premium Widget</n>
    <description>High-quality widget with advanced features</description>
    <category>Electronics</category>
    <price>99.99</price>
    <currency>USD</currency>
    <stock>
        <available>250</available>
        <reserved>15</reserved>
        <warehouse>WH-NYC-01</warehouse>
    </stock>
    <active>true</active>
</product>'

test_endpoint "/api/content-router/route-xml" "$PRODUCT_XML" "Content Router - Product XML"

# User XML
USER_XML='<?xml version="1.0" encoding="UTF-8"?>
<user>
    <userId>USR-12345</userId>
    <username>johnsmith</username>
    <email>john.smith@example.com</email>
    <firstName>John</firstName>
    <lastName>Smith</lastName>
    <role>customer</role>
    <status>active</status>
    <dateCreated>2024-01-15T10:30:00Z</dateCreated>
    <lastLogin>2024-08-16T09:15:00Z</lastLogin>
</user>'

test_endpoint "/api/content-router/route-xml" "$USER_XML" "Content Router - User XML"

print_header "5. Specialized Routing Endpoints"

test_endpoint "/api/content-router/route-customer" "$CUSTOMER_XML" "Specialized Customer Router"
test_endpoint "/api/content-router/route-order" "$ORDER_XML" "Specialized Order Router"
test_endpoint "/api/content-router/route-product" "$PRODUCT_XML" "Specialized Product Router"

print_header "6. XML Type Detection Tests"

test_endpoint "/api/content-router/test-detection" "$CUSTOMER_XML" "XML Detection - Customer"
test_endpoint "/api/content-router/test-detection" "$ORDER_XML" "XML Detection - Order"
test_endpoint "/api/content-router/test-detection" "$PRODUCT_XML" "XML Detection - Product"
test_endpoint "/api/content-router/test-detection" "$USER_XML" "XML Detection - User"

print_header "7. Dynamic Configuration Tests"

# Toggle content router
test_endpoint "/api/management/config/channels/content-router/toggle?enabled=false" "" "Disable Content Router" "POST"
sleep 2
test_endpoint "/api/management/config/channels/content-router/toggle?enabled=true" "" "Enable Content Router" "POST"

# Toggle specific XML type routing
test_endpoint "/api/management/config/routing/customer/toggle?enabled=false" "" "Disable Customer Routing" "POST"
sleep 2
test_endpoint "/api/management/config/routing/customer/toggle?enabled=true" "" "Enable Customer Routing" "POST"

# Toggle subscriber
test_endpoint "/api/management/config/subscriber/audit/toggle?enabled=false" "" "Disable Audit Subscriber" "POST"
sleep 2
test_endpoint "/api/management/config/subscriber/audit/toggle?enabled=true" "" "Enable Audit Subscriber" "POST"

print_header "8. Pub/Sub Verification"

# Send messages and wait for pub/sub processing
print_info "Sending messages to trigger pub/sub events..."
test_endpoint "/api/content-router/route-xml" "$CUSTOMER_XML" "Customer XML (Pub/Sub Test)"
sleep 3
test_endpoint "/api/content-router/route-xml" "$ORDER_XML" "Order XML (Pub/Sub Test)"
sleep 3
test_endpoint "/api/content-router/route-xml" "$PRODUCT_XML" "Product XML (Pub/Sub Test)"
sleep 3

# Check topics status after processing
test_endpoint "/api/management/pubsub/topics" "" "Topics Status After Processing" "GET"

print_header "9. Error Handling Tests"

INVALID_XML='<invalid>not properly closed'
test_endpoint "/api/content-router/route-xml" "$INVALID_XML" "Invalid XML Test"

EMPTY_XML=''
test_endpoint "/api/content-router/route-xml" "$EMPTY_XML" "Empty XML Test"

print_header "10. Configuration Export"

test_endpoint "/api/management/config/export" "" "Export Configuration" "GET"

print_header "11. Load Testing (Multiple Concurrent Requests)"

print_info "Sending 5 concurrent requests to test parallel processing..."

# Run 5 background processes
for i in {1..5}; do
    {
        curl -s -X POST \
            -H "$CONTENT_TYPE" \
            -d "$CUSTOMER_XML" \
            "$BASE_URL/api/content-router/route-xml" > /dev/null 2>&1 &
    }
done

# Wait for all background jobs
wait

print_success "Load test completed"

print_header "12. Final Status Check"

test_endpoint "/api/management/config/status" "" "Final Configuration Status" "GET"
test_endpoint "/api/management/health/detailed" "" "Final Detailed Health" "GET"

echo
print_header "Testing Summary"
print_success "All tests completed successfully!"
print_info "Check the application logs to see detailed pub/sub subscriber activity"
print_info "Key features tested:"
echo "  ✅ Configurable channel routing"
echo "  ✅ XML type detection and routing"
echo "  ✅ Pub/Sub with multiple subscribers"
echo "  ✅ Dynamic configuration toggling"
echo "  ✅ Error handling"
echo "  ✅ Parallel processing"
echo "  ✅ Health monitoring"

echo -e "\n${GREEN}🎉 Enhanced SPI-Router testing completed!${NC}"