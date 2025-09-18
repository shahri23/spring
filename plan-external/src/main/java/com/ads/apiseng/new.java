<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<!DOCTYPE beans [
    <!ENTITY % dummy "">
]>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:int="http://www.springframework.org/schema/integration"
       xmlns:int-http="http://www.springframework.org/schema/integration/http">

    <int:channel id="inputChannel"/>
    <int:channel id="outputChannel"/>

    <int:transformer id="messageTransformer"
                     input-channel="inputChannel"
                     output-channel="outputChannel"
                     expression="'PROCESSED: ' + payload.toString().toUpperCase()"/>

    <int:service-activator input-channel="outputChannel"
                           ref="messageProcessor"
                           method="handle"/>
    
    <bean id="messageProcessor" class="com.ads.apiseng.handlers.SimpleHandler">
        <constructor-arg value="BASIC-PROCESSOR"/>
    </bean>

    <int-http:inbound-gateway id="httpEndpoint"
                              supported-methods="POST"
                              request-channel="inputChannel"
                              path="/api/basic-transform"/>

</beans>