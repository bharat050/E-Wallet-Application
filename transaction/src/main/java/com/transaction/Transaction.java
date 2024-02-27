package com.transaction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Entity
@Data
@Table(name = "transaction")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String transactionId = UUID.randomUUID().toString();

    private String fromName;
    private String toName;
    private double amount;
    private String purpose;

    @Enumerated(value = EnumType.STRING)
    private TransactionStatus status;

//    @CreationTimestamp(source = SourceType.VM)
    private Date tranTime;
}
