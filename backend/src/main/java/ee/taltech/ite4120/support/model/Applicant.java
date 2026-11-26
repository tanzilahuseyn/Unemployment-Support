package ee.taltech.ite4120.support.model;

import java.time.LocalDate;

/**
 * The applicant as shown next to the personal code: first name and surname looked up
 * live from the Population Register, and the date of birth read from the personal
 * code. Never stored. {@code nameAvailable} is false when the registry cannot answer
 * or does not know the person; the date of birth is always there.
 */
public record Applicant(String firstName, String lastName, LocalDate birthDate, boolean nameAvailable) {}
