 
package com.ads.apiseng;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class XmlToJsonTransformer {

    private final XmlMapper xmlMapper;
    private final ObjectMapper objectMapper;

    public XmlToJsonTransformer(XmlMapper xmlMapper, ObjectMapper objectMapper) {
        this.xmlMapper = xmlMapper;
        this.objectMapper = objectMapper;
    }

    public String transform(String xmlData) {
        try {
            // Parse XML to JsonNode
            JsonNode jsonNode = xmlMapper.readTree(xmlData);
            
            // Convert JsonNode to pretty-printed JSON string
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception e) {
            throw new RuntimeException("Error converting XML to JSON: " + e.getMessage(), e);
        }
    }
}
