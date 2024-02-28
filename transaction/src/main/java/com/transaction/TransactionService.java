package com.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
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

    @Autowired
    RestTemplate restTemplate;

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

    @KafkaListener(topics = "update_transaction", groupId = "User_Wallet")
    public void updateTransaction(String message) throws JsonProcessingException {
        JSONObject returnObject = objectMapper.readValue(message, JSONObject.class);

        String transactionId = returnObject.get("transactionId").toString();
        String status = returnObject.get("status").toString();

        Transaction transaction = transactionRepository.findByTransactionId(transactionId);

        transaction.setStatus(TransactionStatus.valueOf(status));
        transactionRepository.save(transaction);

        callNotificationService(transaction);
    }

    public void callNotificationService(Transaction transaction){

        String fromUser = transaction.getFromName();
        String toUser = transaction.getToName();

        URI url = URI.create("http://localhost:8082/getEmail/"+fromUser);
        URI url1 = URI.create("http://localhost:8082/getEmail/"+toUser);

        HttpEntity httpEntity = new HttpEntity(new HttpHeaders());
        JSONObject fromUserJson = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JSONObject.class).getBody();
        JSONObject toUserJson = restTemplate.exchange(url, HttpMethod.GET, httpEntity, JSONObject.class).getBody();
    }
}
