package com.example.demo.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "trades")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlTradeList {

    @XmlElement(name = "trade")
    private List<XmlTrade> trades;
}
