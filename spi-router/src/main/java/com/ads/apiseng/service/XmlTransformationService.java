package com.ads.apiseng.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Service;

@Service
public class XmlTransformationService {
    
    private final XmlMapper xmlMapper = new XmlMapper();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    
    public String transformCustomerXmlToJson(String xml) {
        try {
            JsonNode jsonNode = xmlMapper.readTree(xml);
            String json = jsonMapper.writeValueAsString(jsonNode);
            return "{ \"type\": \"customer\", \"timestamp\": \"" + System.currentTimeMillis() + "\", \"data\": " + json + " }";
        } catch (Exception e) {
            return "{ \"type\": \"customer\", \"error\": \"" + e.getMessage() + "\" }";
        }
    }
    
    public String transformOrderXmlToJson(String xml) {
        try {
            JsonNode jsonNode = xmlMapper.readTree(xml);
            String json = jsonMapper.writeValueAsString(jsonNode);
            return "{ \"type\": \"order\", \"timestamp\": \"" + System.currentTimeMillis() + "\", \"data\": " + json + " }";
        } catch (Exception e) {
            return "{ \"type\": \"order\", \"error\": \"" + e.getMessage() + "\" }";
        }
    }
    
    public String transformProductXmlToJson(String xml) {
        try {
            JsonNode jsonNode = xmlMapper.readTree(xml);
            String json = jsonMapper.writeValueAsString(jsonNode);
            return "{ \"type\": \"product\", \"timestamp\": \"" + System.currentTimeMillis() + "\", \"data\": " + json + " }";
        } catch (Exception e) {
            return "{ \"type\": \"product\", \"error\": \"" + e.getMessage() + "\" }";
        }
    }
    
    public String transformGenericXmlToJson(String xml) {
        try {
            JsonNode jsonNode = xmlMapper.readTree(xml);
            String json = jsonMapper.writeValueAsString(jsonNode);
            return "{ \"type\": \"generic\", \"timestamp\": \"" + System.currentTimeMillis() + "\", \"data\": " + json + " }";
        } catch (Exception e) {
            return "{ \"type\": \"generic\", \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}