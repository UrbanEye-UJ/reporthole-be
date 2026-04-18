package za.co.urbaneye.reporthole.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.urbaneye.reporthole.user.dto.LoginRequest;
import za.co.urbaneye.reporthole.user.dto.RegisterRequest;
import za.co.urbaneye.reporthole.user.service.interfaces.IUserAuthService;

@RestController
@RequestMapping("auth")

@Slf4j
public class AuthServiceController {

    private IUserAuthService service;

    @Autowired
    public AuthServiceController(IUserAuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "Save the user and stores in the database")
    public ResponseEntity<?> save(@RequestBody RegisterRequest request) {
        service.registerUser(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "This generates a token for the user if they exist and are valid")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
      return new ResponseEntity<>(service.loginUser(request), HttpStatus.OK);
    }
}
