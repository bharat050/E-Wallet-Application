package WalletApplication;

import WalletApplication.Model.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.record.UnalignedMemoryRecords;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    WalletRepository walletRepository;
    @KafkaListener(topics = "create_wallet", groupId = "User_Wallet")
    public void createWallet(String message){
        Wallet wallet = Wallet.builder().userName(message).amount(350).build();
        walletRepository.save(wallet);
    }

    @KafkaListener(topics = "update_wallet", groupId = "User_Wallet")
    public void updateWalletFromTransaction(String message) throws JsonProcessingException {
        JSONObject jsonObject = objectMapper.readValue(message, JSONObject.class);

        //retrieving attributes from Object
        String fromUser = jsonObject.get("fromUser").toString();
        String toUser = jsonObject.get("toUser").toString();
        double amount = (double)jsonObject.get("amount");
        String transactionId = jsonObject.get("transactionId").toString();

        // object to be returned back along with status of transaction
        JSONObject returnObject = new JSONObject();
        returnObject.put("transactionId", transactionId);

        // finding which wallet needs to be updated.
        Wallet fromUserWallet = walletRepository.findByUserName(fromUser);
        Wallet toUserWallet = walletRepository.findByUserName(toUser);

        if(amount <= fromUserWallet.getAmount()) {
            fromUserWallet.setAmount(fromUserWallet.getAmount() - amount);
            toUserWallet.setAmount(toUserWallet.getAmount() + amount);

            returnObject.put("status", "SUCCESS");
            kafkaTemplate.send("update_transaction", objectMapper.writeValueAsString(returnObject));

            walletRepository.save(fromUserWallet);
            walletRepository.save(toUserWallet);
        }
        else{
            returnObject.put("status", "FAILED");
            kafkaTemplate.send("update_transaction",objectMapper.writeValueAsString(returnObject));
        }
    }

}
