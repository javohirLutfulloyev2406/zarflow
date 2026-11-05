package uz.zarflow.shared.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Test-only controller exercising every exception path GlobalExceptionHandlerTest asserts on. */
@RestController
class ProbeController {

    @GetMapping("/probe/not-found")
    void notFound() {
        throw new NotFoundException("resource.not-found");
    }

    @GetMapping("/probe/conflict")
    void conflict() {
        throw new ConflictException("probe.conflict");
    }

    @GetMapping("/probe/business-rule")
    void businessRule() {
        throw new BusinessRuleException("probe.business-rule");
    }

    @PostMapping("/probe/validate")
    void validate(@Valid @RequestBody ProbeRequest request) {
    }

    @GetMapping("/probe/boom")
    void boom() {
        throw new RuntimeException("sensitive internal detail");
    }

    record ProbeRequest(@NotBlank String name) {
    }

}
