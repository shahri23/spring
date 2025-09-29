#!/bin/bash

echo "=========================================="
echo "Testing Spring Integration Aggregator"
echo "=========================================="
echo ""

# The aggregator groups messages by correlationId
# Release strategy: size() >= 3 OR timeout after 10 seconds

echo "Test 1: Send 3 messages with same correlationId (should aggregate immediately)"
echo "-----------------------------------------------------------------------------"
echo "Sending Part 1..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -H "Content-Type: text/plain" \
  -d "Part 1" &

echo "Sending Part 2..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -H "Content-Type: text/plain" \
  -d "Part 2" &

echo "Sending Part 3..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-123" \
  -H "Content-Type: text/plain" \
  -d "Part 3" &

wait
echo ""
echo "Expected: All 3 messages should be aggregated and released together"
echo "Check logs for: [AGGREGATION-PROCESSOR] received: [[Part 1], [Part 2], [Part 3]]"
echo ""

sleep 2

echo "Test 2: Send 2 messages with same correlationId (should wait for timeout)"
echo "-----------------------------------------------------------------------------"
echo "Sending Part A..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-456" \
  -H "Content-Type: text/plain" \
  -d "Part A" &

echo "Sending Part B..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-456" \
  -H "Content-Type: text/plain" \
  -d "Part B" &

wait
echo ""
echo "Expected: Messages held until 10-second timeout, then released as partial result"
echo "Note: You'll need to wait 10 seconds to see the aggregation complete"
echo ""

sleep 2

echo "Test 3: Send messages with different correlationIds (should aggregate separately)"
echo "---------------------------------------------------------------------------------"
echo "Sending messages for ORDER-789..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" \
  -H "Content-Type: text/plain" \
  -d "Item 1" &

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" \
  -H "Content-Type: text/plain" \
  -d "Item 2" &

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-789" \
  -H "Content-Type: text/plain" \
  -d "Item 3" &

echo "Sending messages for ORDER-999..."
curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" \
  -H "Content-Type: text/plain" \
  -d "Widget A" &

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" \
  -H "Content-Type: text/plain" \
  -d "Widget B" &

curl -X POST http://localhost:8080/api/advanced/aggregate \
  -H "correlationId: ORDER-999" \
  -H "Content-Type: text/plain" \
  -d "Widget C" &

wait
echo ""
echo "Expected: Two separate aggregations - one for ORDER-789, one for ORDER-999"
echo ""

echo "=========================================="
echo "Tests complete! Check your application logs for:"
echo "  - [AGGREGATION-PROCESSOR] received: ..."
echo "=========================================="