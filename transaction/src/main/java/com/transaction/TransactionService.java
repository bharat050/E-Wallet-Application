package com.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    RedisTemplate<String, Transaction> redisTemplate;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

//    @KafkaListener(topics = "update_wallet", groupId = "User_Wallet")
    public void createTransaction(TransactionRequestDto transactionRequestDto) throws JsonProcessingException {
        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .amount(transactionRequestDto.getAmount())
                .fromName(transactionRequestDto.getFromName())
                .toName(transactionRequestDto.getToName())
                .purpose(transactionRequestDto.getPurpose())
                .status(TransactionStatus.PENDING)
                .tranTime(new Date()).build();

        transactionRepository.save(transaction);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fromUser", transaction.getFromName());
        jsonObject.put("toUser", transaction.getToName());
        jsonObject.put("amount", transaction.getAmount());
        jsonObject.put("transactionId", transaction.getTransactionId());

        String kafkaMessage = objectMapper.writeValueAsString(jsonObject);

        kafkaTemplate.send("update_wallet",kafkaMessage);
    }
}
