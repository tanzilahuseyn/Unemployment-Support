package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.Checks;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.SupportType;
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

/** The unemployment support REST API — SUPPORT.01, API. HTTP only; the rules are in ApplicationService. */
@RestController
@RequestMapping("/api/support")
@Tag(name = "Unemployment support", description = "Applications decided from live X-Road registry checks")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "Search applications — paginated, filterable by status and support type")
    @ApiResponse(responseCode = "400", description = "Unknown sort key")
    @GetMapping("/applications")
    public QueryResult<Application> search(@ModelAttribute ApplicationQueryParams params) {
        return service.search(params);
    }

    @Operation(summary = "One application by id")
    @ApiResponse(responseCode = "404", description = "No active application with that id")
    @GetMapping("/applications/{id}")
    public Application byId(@PathVariable Long id) {
        return service.require(id);
    }

    @Operation(summary = "Register an application")
    @ApiResponse(responseCode = "201", description = "Created — Location header points at the new application")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "The person already has an open application for an overlapping period")
    @PostMapping("/applications")
    public ResponseEntity<Application> create(@Valid @RequestBody ApplicationRequest body) {
        Application saved = service.create(body.toModel());
        return ResponseEntity.created(URI.create("/api/support/applications/" + saved.getId())).body(saved);
    }

    @Operation(summary = "Update an open application (the personal code is immutable)")
    @ApiResponse(responseCode = "409", description = "Already decided, or an overlapping open application")
    @PutMapping("/applications/{id}")
    public Application update(@PathVariable Long id, @Valid @RequestBody ApplicationRequest body) {
        return service.update(id, body.toModel());
    }

    @Operation(summary = "Withdraw an open application (soft delete)")
    @ApiResponse(responseCode = "204", description = "Withdrawn")
    @ApiResponse(responseCode = "409", description = "Already decided")
    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "The three registry checks, live — never stored",
            description = "Viewing fails open: an unreachable registry is reported as UNAVAILABLE and the "
                    + "other checks are still answered.")
    @GetMapping("/applications/{id}/checks")
    public Checks checks(@PathVariable Long id) {
        return service.checks(id);
    }

    @Operation(summary = "Decide an application",
            description = "The server runs the three checks itself. Approving while a check did not pass "
                    + "needs override=true and a reason; rejecting always needs a reason.")
    @ApiResponse(responseCode = "201", description = "Decided")
    @ApiResponse(responseCode = "400", description = "Missing reason, or approval without override while a check did not pass")
    @ApiResponse(responseCode = "409", description = "Already decided")
    @PostMapping("/applications/{id}/decision")
    public ResponseEntity<Decision> decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
        Decision d = service.decide(id, body.getOutcome(), body.isOverride(), body.getReason());
        return ResponseEntity.created(URI.create("/api/support/applications/" + id + "/decision")).body(d);
    }

    @Operation(summary = "The decision of a decided application")
    @ApiResponse(responseCode = "404", description = "No such application, or not decided yet")
    @GetMapping("/applications/{id}/decision")
    public Decision decision(@PathVariable Long id) {
        return service.decision(id);
    }

    @Operation(summary = "Support type reference list")
    @GetMapping("/types")
    public List<SupportType> types() {
        return service.types();
    }
}
