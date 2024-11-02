package com.sang.v2;


import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

@Component
public class XmlRpcConverter {
    
    public String createXmlRpcRequest(String methodName, Map<String, String> params) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            Element methodCall = doc.createElement("methodCall");
            doc.appendChild(methodCall);
            
            Element methodNameElement = doc.createElement("methodName");
            methodNameElement.setTextContent(methodName);
            methodCall.appendChild(methodNameElement);
            
            Element parameters = doc.createElement("params");
            methodCall.appendChild(parameters);
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                Element param = doc.createElement("param");
                parameters.appendChild(param);
                
                Element value = doc.createElement("value");
                param.appendChild(value);
                
                Element struct = doc.createElement("struct");
                value.appendChild(struct);
                
                Element member = doc.createElement("member");
                struct.appendChild(member);
                
                Element name = doc.createElement("name");
                name.setTextContent(entry.getKey());
                member.appendChild(name);
                
                Element memberValue = doc.createElement("value");
                member.appendChild(memberValue);
                
                Element string = doc.createElement("string");
                string.setTextContent(entry.getValue());
                memberValue.appendChild(string);
            }
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XML-RPC request", e);
        }
    }
    
    public Map<String, Object> parseXmlRpcResponse(String xmlResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            
            NodeList valueNodes = doc.getElementsByTagName("value");
            for (int i = 0; i < valueNodes.getLength(); i++) {
                Element valueElement = (Element) valueNodes.item(i);
                if (valueElement.getParentNode().getNodeName().equals("param")) {
                    return parseValue(valueElement);
                }
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML-RPC response", e);
        }
    }

    public Map<String, Object> parseRawXmlRpcRequest(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            Map<String, Object> result = new HashMap<>();

            // Get method name
            NodeList methodNameNodes = doc.getElementsByTagName("methodName");
            if (methodNameNodes.getLength() > 0) {
                result.put("methodName", methodNameNodes.item(0).getTextContent());
            } else {
                throw new RuntimeException("No methodName found in XML-RPC request");
            }

            // Get parameters
            Map<String, String> params = new HashMap<>();
            NodeList memberNodes = doc.getElementsByTagName("member");
            
            for (int i = 0; i < memberNodes.getLength(); i++) {
                Element memberElement = (Element) memberNodes.item(i);
                String paramName = memberElement.getElementsByTagName("name")
                    .item(0).getTextContent();
                
                NodeList valueNodes = memberElement.getElementsByTagName("value");
                if (valueNodes.getLength() > 0) {
                    Element valueElement = (Element) valueNodes.item(0);
                    String paramValue = extractValueContent(valueElement);
                    params.put(paramName, paramValue);
                }
            }
            
            result.put("params", params);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML-RPC request", e);
        }
    }
    
    private Map<String, Object> parseValue(Element valueElement) {
        NodeList children = valueElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                
                switch (nodeName) {
                    case "struct":
                        return parseStruct((Element) child);
                    case "string":
                        return Map.of("result", child.getTextContent());
                    default:
                        return Map.of("result", child.getTextContent());
                }
            }
        }
        return new HashMap<>();
    }
    
    private Map<String, Object> parseStruct(Element structElement) {
        Map<String, Object> result = new HashMap<>();
        NodeList memberElements = structElement.getElementsByTagName("member");
        
        for (int i = 0; i < memberElements.getLength(); i++) {
            Element memberElement = (Element) memberElements.item(i);
            String name = memberElement.getElementsByTagName("name").item(0).getTextContent();
            Element valueElement = (Element) memberElement.getElementsByTagName("value").item(0);
            
            NodeList valueChildren = valueElement.getChildNodes();
            for (int j = 0; j < valueChildren.getLength(); j++) {
                Node valueChild = valueChildren.item(j);
                if (valueChild.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(name, valueChild.getTextContent());
                    break;
                }
            }
        }
        
        return result;
    }

    private String extractValueContent(Element valueElement) {
        NodeList children = valueElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                switch (nodeName) {
                    case "string":
                    case "i4":
                    case "int":
                    case "double":
                    case "boolean":
                        return child.getTextContent();
                    default:
                        return child.getTextContent();
                }
            }
        }
        return valueElement.getTextContent().trim();
    }
}