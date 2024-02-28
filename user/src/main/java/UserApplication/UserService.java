package UserApplication;

import UserApplication.Model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    @Autowired
    RedisTemplate<String, User> redisTemplate;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    public String addUser(UserRequestDto userRequestDto) {
        // need to save in DB & Redis.

        User user = User.builder()
                .age(userRequestDto.getAge())
                .name(userRequestDto.getName())
                .mobileNo(userRequestDto.getMobileNo())
                .email(userRequestDto.getEmail()).build();

        userRepository.save(user);

        saveInCache(user);

        // When dATA saved in db we need to update the information into the Wallet module. here we're sending the name.
        kafkaTemplate.send("create_wallet", user.getName());

        return "User Created Successfully";
    }

    public User getUser(String name) {
        // first check in Redis if available -> get it
        // if not -> fetch it from DB and save that entity into the Redis.
        String key = "UserKey_" + name;

        Map obj = redisTemplate.opsForHash().entries(key);

        // checking if cache has this key for faster retrieval
        if(!obj.isEmpty()) return objectMapper.convertValue(obj, User.class);

        // getting from the DB
        User user = userRepository.findByName(name);

        //converting to map and save in cache
        saveInCache(user);

        return user;
    }

    public void saveInCache(User user){
        Map map = objectMapper.convertValue(user, Map.class);
        String key = "UserKey_" + user.getName();
        System.out.println(key);
        redisTemplate.opsForHash().putAll(key, map);

        redisTemplate.expire(key, Duration.ofHours(12));
    }

    public UserResponseDto getEmailDetailsByName(String name) {
        User user = userRepository.findByName(name);
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .name(user.getName()).email(user.getEmail()).build();

        return userResponseDto;
    }
}
