package com.sang.v2;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProxyController {
    private final String XML_RPC_SERVER = "http://localhost:1111/api/xmlrpc";
    private final RestTemplate restTemplate;
    private final XmlRpcConverter xmlRpcConverter;

    public ProxyController(RestTemplate restTemplate, XmlRpcConverter xmlRpcConverter) {
        this.restTemplate = restTemplate;
        this.xmlRpcConverter = xmlRpcConverter;
    }

    @PostMapping(value = "/rest/fromxml", 
                consumes = MediaType.TEXT_XML_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleXmlRequest(@RequestBody String xmlRequest) {
        try {
            Map<String, Object> parsedRequest = xmlRpcConverter.parseRawXmlRpcRequest(xmlRequest);
            String methodName = (String) parsedRequest.get("methodName");
            @SuppressWarnings("unchecked")
            Map<String, String> params = (Map<String, String>) parsedRequest.get("params");

            switch(methodName) {
                case "hello":
                    return ResponseEntity.ok(getHello(params.get("name")));
                case "input":
                    return ResponseEntity.ok(postInput(params.get("input")));
                case "sua":
                    return ResponseEntity.ok(getSua(params.get("name")));
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Unknown method: " + methodName));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process request: " + e.getMessage()));
        }
    }

    @GetMapping(value = "/rest/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHello(@RequestParam(required = false) String name) {
        String xmlRequest = xmlRpcConverter.createXmlRpcRequest("hello", 
            Map.of("name", name != null ? name : ""));
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            XML_RPC_SERVER, 
            xmlRequest, 
            String.class
        );
        
        return xmlRpcConverter.parseXmlRpcResponse(response.getBody());
    }

    @PostMapping(value = "/rest/input", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> postInput(@RequestBody String input) {
        String xmlRequest = xmlRpcConverter.createXmlRpcRequest("input", 
            Map.of("input", input));
            
        ResponseEntity<String> response = restTemplate.postForEntity(
            XML_RPC_SERVER,
            xmlRequest,
            String.class
        );
        
        return xmlRpcConverter.parseXmlRpcResponse(response.getBody());
    }

    @GetMapping(value = "/rest/sua", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getSua(@RequestParam(required = false) String name) {
        String xmlRequest = xmlRpcConverter.createXmlRpcRequest("sua",
            Map.of("name", name != null ? name : "minh"));
            
        ResponseEntity<String> response = restTemplate.postForEntity(
            XML_RPC_SERVER,
            xmlRequest,
            String.class
        );
        
        return xmlRpcConverter.parseXmlRpcResponse(response.getBody());
    }
}