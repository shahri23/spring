package com.ads.apiseng;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface XmlToJsonGateway {
    
    @Gateway(requestChannel = "xmlInputChannel")
    String convertXmlToJson(String xmlData);
}
