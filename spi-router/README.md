 
## Original Pattern (unchanged):

Endpoint: Your existing XML→JSON transformation
Channel: xmlInputChannel
Uses: Your existing XmlToJsonTransformer

## New Content-Based Router Pattern:

Endpoint: /api/content-router/route-xml
Channel: contentRouterInputChannel
Routes based on XML content type
Specialized transformations for each type

# Your existing endpoint still works unchanged

curl -X POST http://localhost:8080/api/transform      -H "Content-Type: application/xml"  -d @samples/sample.xml


# New Content-Based Router:
curl -X POST http://localhost:8080/api/content-router/route-xml  -H "Content-Type: application/xml"  -d '<customer><id>123</id><name>John</name></customer>'



  curl -X POST http://localhost:8080/api/content-router/route-xml \
  -H "Content-Type: application/xml" \
  -d 