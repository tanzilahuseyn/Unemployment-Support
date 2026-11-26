package ee.taltech.ite4120.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Development-only authentication: the Bearer token value IS the username.
 * {@code Authorization: Bearer alice} signs you in as {@code alice}. Nothing is
 * verified — which is the point, and why {@code auth.mock.enabled} defaults to
 * {@code false} and must never be true outside local development.
 *
 * <p>Wire-compatible with the Helex platform's {@code commons-mock-auth}
 * ({@code MockSessionProvider}), so the same frontend auth provider and the same
 * curl commands work against this template and against a real EMR module.
 */
@Component
public class MockBearerAuthFilter extends OncePerRequestFilter {

    @Value("${auth.mock.enabled:false}")
    private boolean mockEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (mockEnabled && header != null && header.startsWith("Bearer ")) {
            String username = header.substring("Bearer ".length()).trim();
            // `citizen:<personal code>` is a citizen (a demo of the state login, TARA);
            // any other name is a social worker. A citizen with an invalid code is not
            // signed in at all, so the request answers 401.
            String role = CitizenIdentity.isCitizen(username) ? "ROLE_CITIZEN" : "ROLE_USER";
            boolean valid = !username.isEmpty()
                    && (!CitizenIdentity.isCitizen(username) || CitizenIdentity.personalCode(username) != null);
            if (valid) {
                var auth = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority(role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
