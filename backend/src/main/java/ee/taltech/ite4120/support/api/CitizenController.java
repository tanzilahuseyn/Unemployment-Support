package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.config.CitizenIdentity;
import ee.taltech.ite4120.support.model.Applicant;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.BenefitScheme;
import ee.taltech.ite4120.support.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The citizen e-service — SUPPORT.01, Citizen e-service. Only for the role CITIZEN;
 * the personal code always comes from the login. HTTP only; the rules are in
 * ApplicationService, the same ones the worker's register uses.
 */
@RestController
@RequestMapping("/api/citizen")
@Tag(name = "Citizen e-service", description = "Apply online. Sign in with Bearer citizen:<personal code> (demo of the state login)")
public class CitizenController {

    /** What the citizen gets back after applying: the receipt, no internal fields. */
    public record Receipt(Long id, String supportTypeCode, LocalDate periodStart, LocalDate periodEnd,
            Integer householdSize, ApplicationStatus status) {}

    /** The signed-in person; the name is live from the Population Register and never stored. */
    public record Me(String personalCode, String firstName, String lastName, LocalDate birthDate, boolean nameAvailable) {}

    private final ApplicationService service;

    public CitizenController(ApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "The signed-in person: personal code, name (live, not stored) and date of birth")
    @GetMapping("/me")
    public Me me() {
        String code = CitizenIdentity.currentPersonalCode();
        Applicant a = service.citizen(code);
        return new Me(code, a.firstName(), a.lastName(), a.birthDate(), a.nameAvailable());
    }

    @Operation(summary = "Effective benefit schemes")
    @GetMapping("/schemes")
    public List<BenefitScheme> schemes() {
        return service.schemes();
    }

    @Operation(summary = "Apply online for yourself")
    @ApiResponse(responseCode = "201", description = "Received — the application number and what was applied for")
    @ApiResponse(responseCode = "400", description = "Validation failed, e.g. the period ends before it starts")
    @ApiResponse(responseCode = "409", description = "You already have an application for an overlapping period that is being processed")
    @PostMapping("/applications")
    public ResponseEntity<Receipt> apply(@Valid @RequestBody OnlineApplicationRequest body) {
        Application a = service.applyOnline(body.toModel(CitizenIdentity.currentPersonalCode()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new Receipt(a.getId(), a.getSupportTypeCode(),
                a.getPeriodStart(), a.getPeriodEnd(), a.getHouseholdSize(), a.getStatus()));
    }
}
