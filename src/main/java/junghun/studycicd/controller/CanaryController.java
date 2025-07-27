package junghun.studycicd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/canary")
public class CanaryController {

    private boolean fail = false;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/fail")
    public ResponseEntity<String> fail() {
        return ResponseEntity.internalServerError()
                             .body("fail");
    }

    @GetMapping("/work")
    public ResponseEntity<String> work() {
        if (fail) {
            return ResponseEntity.internalServerError()
                                 .body("work fail");
        }
        return ResponseEntity.ok("work ok");
    }

    @GetMapping("/fail/active")
    public ResponseEntity<String> activeFail() {
        this.fail = true;
        return ResponseEntity.ok("fail active");
    }
}
