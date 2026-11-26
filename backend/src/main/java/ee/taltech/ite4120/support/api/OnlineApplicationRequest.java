package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.model.Application;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * A citizen's online application. There is no personal code field on purpose: it
 * comes from the login, so a citizen can only apply for themselves (a personal code
 * sent in the body is ignored).
 */
public record OnlineApplicationRequest(
        @NotBlank String supportTypeCode,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull @Min(1) @Max(20) Integer householdSize) {

    public Application toModel(String personalCode) {
        Application a = new Application();
        a.setApplicantIsikukood(personalCode);
        a.setSupportTypeCode(supportTypeCode);
        a.setPeriodStart(periodStart);
        a.setPeriodEnd(periodEnd);
        a.setHouseholdSize(householdSize);
        return a;
    }
}
