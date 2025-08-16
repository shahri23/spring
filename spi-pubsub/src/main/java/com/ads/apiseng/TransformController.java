 
package com.ads.apiseng;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransformController {

    @Autowired
    private XmlToJsonGateway xmlToJsonGateway;

    @PostMapping(value = "/transform", 
                 consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE},
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> transformXmlToJson(@RequestBody String xmlData) {
        try {
            String jsonResult = xmlToJsonGateway.convertXmlToJson(xmlData);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            String errorJson = String.format("{\"error\":\"%s\"}", 
                e.getMessage().replace("\"", "\\\""));
            return ResponseEntity.badRequest().body(errorJson);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"service\":\"SpiApp XML to JSON Converter\"}");
    }
}