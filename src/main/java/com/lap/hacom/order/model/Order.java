package com.lap.hacom.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private ObjectId _id;

    private String orderId;
    private String customerId;
    private String customerPhoneNumber;
    private String status;
    private List<String> items;
    private OffsetDateTime ts;
}

