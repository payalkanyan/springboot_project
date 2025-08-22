package com.example.demo.xml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "trade")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlTrade {

    @XmlElement(name = "tradeId")
    private String tradeId;
    @XmlElement(name = "instrument")
    private String instrument;
    @XmlElement(name = "quantity")
    private BigDecimal quantity;
    @XmlElement(name = "price")
    private BigDecimal price;
    @XmlElement(name = "tradeDate")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate tradeDate;
    @XmlElement(name = "settlementDate")
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate settlementDate;
    @XmlElement(name = "buyer")
    private String buyer;
    @XmlElement(name = "seller")
    private String seller;
    @XmlElement(name = "currency")
    private String currency;
}
