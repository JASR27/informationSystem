package com.maruseron.informationSystem.presentation;

import com.maruseron.informationSystem.application.EmployeeService;
import com.maruseron.informationSystem.application.dto.AuthDTO;
import com.maruseron.informationSystem.util.Controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<?> auth(@RequestBody AuthDTO request) {
        return Controllers.handleResult(employeeService.auth(request),
                r -> ResponseEntity.ok().build());
    }
}
