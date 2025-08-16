#!/bin/bash

# 🧪 Automated Test Script for SPI-PubSub Application
# This script runs the most important tests automatically

echo "🚀 Starting SPI-PubSub Application Tests..."
echo "=========================================="

BASE_URL="http://localhost:8080"
PASS_COUNT=0
FAIL_COUNT=0

# Function to run test and check response
run_test() {
    local test_name="$1"
    local curl_command="$2"
    local expected_pattern="$3"
    
    echo -n "Testing: $test_name... "
    
    response=$(eval $curl_command 2>/dev/null)
    exit_code=$?
    
    if [ $exit_code -eq 0 ] && echo "$response" | grep -q "$expected_pattern"; then
        echo "✅ PASS"
        ((PASS_COUNT++))
        return 0
    else
        echo "❌ FAIL"
        echo "   Response: $response"
        ((FAIL_COUNT++))
        return 1
    fi
}

# Test 1: Health Check
run_test "Health Check" \
    "curl -s $BASE_URL/api/management/health" \
    '"status":"UP"'

# Test 2: Configuration Status
run_test "Configuration Status" \
    "curl -s $BASE_URL/api/management/config/status" \
    '"routing":'

# Test 3: Enable PubSub
run_test "Enable PubSub" \
    "curl -s -X POST '$BASE_URL/api/management/config/pubsub/toggle?enabled=true'" \
    '"status":"success"'

# Test 4: PubSub Status Check
run_test "PubSub Status" \
    "curl -s $BASE_URL/api/management/pubsub/status" \
    '"status":"active"'

# Test 5: Router Status
run_test "Router Status" \
    "curl -s $BASE_URL/api/router/status" \
    '"status":"UP"'

# Test 6: XML Transformation - Customer
run_test "Customer XML Transformation" \
    "curl -s -X POST $BASE_URL/api/transform -H 'Content-Type: application/xml' -d '<customer><id>123</id><name>Test User</name></customer>'" \
    '"id"'

# Test 7: XML Transformation - Order
run_test "Order XML Transformation" \
    "curl -s -X POST $BASE_URL/api/transform -H 'Content-Type: application/xml' -d '<order><orderId>ORD-001</orderId><status>confirmed</status></order>'" \
    '"orderId"'

# Test 8: Content Routing
run_test "Content Routing" \
    "curl -s -X POST $BASE_URL/api/router/route -H 'Content-Type: application/json' -d '{\"type\":\"customer\",\"data\":\"test\"}'" \
    '"status"'

# Test 9: Configuration Export
run_test "Configuration Export" \
    "curl -s $BASE_URL/api/management/config/export" \
    '"channels":'

# Test 10: Invalid Endpoint (should return 404)
run_test "404 Error Handling" \
    "curl -s $BASE_URL/api/invalid-endpoint" \
    '"status":404'

echo ""
echo "=========================================="
echo "🎯 Test Results Summary:"
echo "✅ Passed: $PASS_COUNT tests"
echo "❌ Failed: $FAIL_COUNT tests"
echo "📊 Success Rate: $(( PASS_COUNT * 100 / (PASS_COUNT + FAIL_COUNT) ))%"

if [ $FAIL_COUNT -eq 0 ]; then
    echo "🎉 All tests passed! Your application is working perfectly!"
    exit 0
else
    echo "⚠️  Some tests failed. Check the application logs and retry."
    exit 1
fi
