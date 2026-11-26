package ee.taltech.ite4120.config;

import ee.taltech.ite4120.support.service.PersonalCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Who a citizen is (SUPPORT.01, Citizen e-service). In mock auth the token
 * {@code citizen:<personal code>} stands in for the state login (TARA), which in
 * production hands over the personal code.
 */
public final class CitizenIdentity {

    public static final String PREFIX = "citizen:";

    private CitizenIdentity() {
    }

    public static boolean isCitizen(String username) {
        return username != null && username.startsWith(PREFIX);
    }

    /** The personal code in a citizen username, or null when it is not a valid Estonian code. */
    public static String personalCode(String username) {
        if (!isCitizen(username)) {
            return null;
        }
        String code = username.substring(PREFIX.length());
        return PersonalCode.isValid(code) ? code : null;
    }

    /** The signed-in citizen's personal code; only ever called behind hasRole("CITIZEN"). */
    public static String currentPersonalCode() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String code = auth == null ? null : personalCode(auth.getName());
        if (code == null) {
            throw new IllegalStateException("no citizen is signed in");
        }
        return code;
    }
}
