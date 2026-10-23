package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.AuditEvent;
import ee.taltech.ite4120.support.model.BenefitScheme;
import ee.taltech.ite4120.support.model.CheckType;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.Evidence;
import ee.taltech.ite4120.support.model.Verification;
import ee.taltech.ite4120.support.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.helex.commons.model.QueryResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The unemployment support REST API — SUPPORT.01 revision 2, API. HTTP only; the rules are in ApplicationService. */
@RestController
@RequestMapping("/api/support")
@Tag(name = "Unemployment support", description = "Applications verified against X-Road registries and decided with reasons")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Search applications — a search by personal code is audited")
    @ApiResponse(responseCode = "400", description = "Unknown sort key")
    @GetMapping("/applications")
    public QueryResult<Application> search(@ModelAttribute ApplicationQueryParams params) {
        return service.search(params);
    }

    @Operation(summary = "One application by id — audited as a view")
    @ApiResponse(responseCode = "404", description = "No active application with that id")
    @GetMapping("/applications/{id}")
    public Application byId(@PathVariable Long id) {
        return service.view(id);
    }

    @Operation(summary = "Register an application")
    @ApiResponse(responseCode = "201", description = "Created — Location header points at the new application")
    @ApiResponse(responseCode = "400", description = "Validation failed, e.g. not a valid Estonian personal code")
    @ApiResponse(responseCode = "409", description = "An undecided application for an overlapping period exists; the message names it")
    @PostMapping("/applications")
    public ResponseEntity<Application> create(@Valid @RequestBody ApplicationRequest body) {
        Application saved = service.create(body.toModel());
        return ResponseEntity.created(URI.create("/api/support/applications/" + saved.getId())).body(saved);
    }

    @Operation(summary = "Update an undecided application; it returns to OPEN and must be verified again")
    @ApiResponse(responseCode = "409", description = "Already decided, or an overlapping undecided application")
    @PutMapping("/applications/{id}")
    public Application update(@PathVariable Long id, @Valid @RequestBody ApplicationRequest body) {
        return service.update(id, body.toModel());
    }

    @Operation(summary = "Withdraw an undecided application (soft delete)")
    @ApiResponse(responseCode = "204", description = "Withdrawn")
    @ApiResponse(responseCode = "409", description = "Already decided")
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verify: query the registries the scheme requires and store the evidence",
            description = "Answers the stored evidence and the new status, plus the person's name and address "
                    + "for display — those are not stored. An unreachable registry is one UNAVAILABLE check.")
    @ApiResponse(responseCode = "409", description = "Already decided")
    @PostMapping("/applications/{id}/verification")
    public Verification verify(@PathVariable Long id) {
        return service.verify(id);
    }

    @Operation(summary = "The current evidence per check (stored data only)")
    @GetMapping("/applications/{id}/evidence")
    public List<Evidence> evidence(@PathVariable Long id) {
        return service.evidence(id);
    }

    @Operation(summary = "Manually verify an unavailable or inconclusive check from another authorised source")
    @ApiResponse(responseCode = "400", description = "Result is not PASS/FAIL, or source or note missing")
    @ApiResponse(responseCode = "409", description = "The check is not unresolved, or the application is decided")
    @PostMapping("/applications/{id}/evidence/{checkType}/manual")
    public Verification verifyManually(@PathVariable Long id, @PathVariable CheckType checkType,
            @Valid @RequestBody ManualVerificationRequest body) {
        return service.verifyManually(id, checkType, body.getResult(), body.getSource(), body.getNote());
    }

    @Operation(summary = "Decide an application from the stored evidence",
            description = "Approve only when READY_FOR_DECISION; a verified adverse fact needs exception=true and an "
                    + "explanation. Reject needs a reason code and an explanation; a registry reason needs that fact "
                    + "verified adverse. The legal basis, rule version and review information come from the scheme.")
    @ApiResponse(responseCode = "201", description = "Decided")
    @ApiResponse(responseCode = "400", description = "A decision rule was broken")
    @ApiResponse(responseCode = "409", description = "Already decided")
    @PostMapping("/applications/{id}/decision")
    public ResponseEntity<Decision> decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
        Decision d = service.decide(id, body.toCommand());
        return ResponseEntity.created(URI.create("/api/support/applications/" + id + "/decision")).body(d);
    }

    @Operation(summary = "The decision of a decided application, with the evidence it relied on")
    @ApiResponse(responseCode = "404", description = "No such application, or not decided yet")
    @GetMapping("/applications/{id}/decision")
    public Decision decision(@PathVariable Long id) {
        return service.decision(id);
    }

    @Operation(summary = "The application's audit events")
    @GetMapping("/applications/{id}/audit")
    public List<AuditEvent> audit(@PathVariable Long id) {
        return service.audit(id);
    }

    @Operation(summary = "Effective benefit schemes with their legal basis and the facts their rules require")
    @GetMapping("/schemes")
    public List<BenefitScheme> schemes() {
        return service.schemes();
    }
}
