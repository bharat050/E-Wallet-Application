package UserApplication;

import UserApplication.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;
    @PostMapping("/add-user")
    public String addUser(@RequestBody UserRequestDto userRequestDto){
        return userService.addUser(userRequestDto);
    }

    @GetMapping("/get-user/{name}")
    public User getUser(@PathVariable("name") String name){
        return userService.getUser(name);
    }

    @GetMapping("/getEmail/{name}")
    public UserResponseDto getEmailDetailsByName(@PathVariable("name") String name){
        return userService.getEmailDetailsByName(name);
    }
}
