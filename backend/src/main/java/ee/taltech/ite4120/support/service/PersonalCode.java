package ee.taltech.ite4120.support.service;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * The official Estonian personal code (isikukood): 11 digits, a first digit 1–8
 * giving sex and century, a real birth date, a serial number, and a checksum
 * computed with two weight sets (EVS 585).
 */
public final class PersonalCode {

    private static final int[] WEIGHTS_1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
    private static final int[] WEIGHTS_2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};

    private PersonalCode() {}

    public static boolean isValid(String code) {
        if (code == null || !code.matches("\\d{11}")) {
            return false;
        }
        int first = code.charAt(0) - '0';
        if (first < 1 || first > 8) {
            return false;
        }
        int century = 1800 + ((first - 1) / 2) * 100;
        try {
            LocalDate.of(century + Integer.parseInt(code.substring(1, 3)),
                    Integer.parseInt(code.substring(3, 5)), Integer.parseInt(code.substring(5, 7)));
        } catch (DateTimeException e) {
            return false;
        }
        return checksum(code) == code.charAt(10) - '0';
    }

    static int checksum(String code) {
        int sum = weighted(code, WEIGHTS_1) % 11;
        if (sum == 10) {
            sum = weighted(code, WEIGHTS_2) % 11;
            if (sum == 10) {
                sum = 0;
            }
        }
        return sum;
    }

    private static int weighted(String code, int[] weights) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (code.charAt(i) - '0') * weights[i];
        }
        return sum;
    }
}
