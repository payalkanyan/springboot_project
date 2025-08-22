package com.example.demo.xml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.StringWriter;

@Service
public class XmlService {

    private JAXBContext jaxbContext;

    public XmlService() throws JAXBException {
        jaxbContext = JAXBContext.newInstance(XmlTradeList.class, XmlTrade.class);
    }

    public String marshal(XmlTradeList tradeList) throws JAXBException {
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(tradeList, sw);
        return sw.toString();
    }

    public XmlTradeList unmarshal(String xmlString) throws JAXBException {
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        StringReader reader = new StringReader(xmlString);
        return (XmlTradeList) jaxbUnmarshaller.unmarshal(reader);
    }
}
