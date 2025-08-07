package com.ads.apiseng.util;

import org.springframework.stereotype.Component;

@Component
public class XmlTypeDetector {
    
    public static String detectType(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.trim().isEmpty()) {
            return "INVALID";
        }
        
        // Convert to lowercase for case-insensitive matching
        String xml = xmlPayload.toLowerCase().trim();
        
        // Check for customer XML patterns
        if (xml.contains("<customer") || xml.contains("customer>") || 
            xml.contains("<cust") || xml.contains("cust>")) {
            return "CUSTOMER";
        }
        
        // Check for order XML patterns  
        if (xml.contains("<order") || xml.contains("order>") ||
            xml.contains("<ord") || xml.contains("ord>")) {
            return "ORDER";
        }
        
        // Check for product XML patterns
        if (xml.contains("<product") || xml.contains("product>") ||
            xml.contains("<prod") || xml.contains("prod>") ||
            xml.contains("<item") || xml.contains("item>")) {
            return "PRODUCT";
        }
        
        // Check for invoice XML patterns
        if (xml.contains("<invoice") || xml.contains("invoice>") ||
            xml.contains("<inv") || xml.contains("inv>")) {
            return "INVOICE";
        }
        
        // Check for user/employee XML patterns
        if (xml.contains("<user") || xml.contains("user>") ||
            xml.contains("<employee") || xml.contains("employee>") ||
            xml.contains("<person") || xml.contains("person>")) {
            return "USER";
        }
        
        return "GENERIC";
    }
    
    public static boolean isValidXml(String xmlPayload) {
        if (xmlPayload == null || xmlPayload.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = xmlPayload.trim();
        return trimmed.startsWith("<") && trimmed.endsWith(">");
    }
    
    public static String extractRootElement(String xmlPayload) {
        if (!isValidXml(xmlPayload)) {
            return "unknown";
        }
        
        String trimmed = xmlPayload.trim();
        int start = trimmed.indexOf('<') + 1;
        int end = trimmed.indexOf('>', start);
        
        if (end > start) {
            String rootTag = trimmed.substring(start, end);
            // Remove attributes if any
            int spaceIndex = rootTag.indexOf(' ');
            if (spaceIndex > 0) {
                rootTag = rootTag.substring(0, spaceIndex);
            }
            return rootTag.toLowerCase();
        }
        
        return "unknown";
    }
}