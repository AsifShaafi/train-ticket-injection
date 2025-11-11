package auth.controller;


import auth.dto.BasicAuthDto;
import auth.entity.FaultInjectionResponse;
import auth.entity.User;
import auth.exception.UserOperationException;
import auth.service.TokenService;
import auth.service.UserService;
import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/hello")
    public Object getHello() {
        return "Hello";
    }

    @PostMapping("/login")
    public ResponseEntity<Response> getToken(@RequestBody BasicAuthDto dao , @RequestHeader HttpHeaders headers) {
        logger.info("Login request of username: {}", dao.getUsername());
        
        // INJECTED FAULT: Validate username format (length must be between 4 and 20 characters)
        if (dao.getUsername() == null || dao.getUsername().trim().isEmpty()) {
            logger.warn("[getToken][INJECTED FAULT][INVALID_USERNAME_FORMAT_FAULT] username is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_USERNAME_FORMAT_FAULT", 
                "Login rejected: username cannot be null or empty"
            );
            return ResponseEntity.badRequest().body(new Response<>(0, "Login rejected: username cannot be null or empty", faultResponse));
        }
        
        if (dao.getUsername().trim().length() < 4 || dao.getUsername().trim().length() > 20) {
            logger.warn("[getToken][INJECTED FAULT][INVALID_USERNAME_FORMAT_FAULT] username length invalid: {}", dao.getUsername().trim().length());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_USERNAME_FORMAT_FAULT", 
                "Login rejected: username length must be between 4 and 20 characters",
                String.format("Username length: %d", dao.getUsername().trim().length())
            );
            return ResponseEntity.badRequest().body(new Response<>(0, "Login rejected: username length must be between 4 and 20 characters", faultResponse));
        }
        
        // INJECTED FAULT: Validate password length (must be at least 6 characters)
        if (dao.getPassword() == null || dao.getPassword().length() < 6) {
            logger.warn("[getToken][INJECTED FAULT][INVALID_PASSWORD_LENGTH_FAULT] password is null or too short");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_PASSWORD_LENGTH_FAULT", 
                "Login rejected: password must be at least 6 characters",
                dao.getPassword() == null ? "null" : String.format("Password length: %d", dao.getPassword().length())
            );
            return ResponseEntity.badRequest().body(new Response<>(0, "Login rejected: password must be at least 6 characters", faultResponse));
        }
        
        try {
            Response<?> res = tokenService.getToken(dao, headers);
            return ResponseEntity.ok(res);
        } catch (UserOperationException e) {
            logger.error("[getToken][tokenService.getToken error][UserOperationException, message: {}]", e.getMessage());
            return ResponseEntity.ok(new Response<>(0, "get token error", null));
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUser(@RequestHeader HttpHeaders headers) {
        logger.info("[getAllUser][Get all users]");
        return ResponseEntity.ok().body(userService.getAllUser(headers));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Response> deleteUserById(@PathVariable String userId, @RequestHeader HttpHeaders headers) {
        logger.info("[deleteUserById][Delete user][userId: {}]", userId);
        return ResponseEntity.ok(userService.deleteByUserId(userId, headers));
    }

}
