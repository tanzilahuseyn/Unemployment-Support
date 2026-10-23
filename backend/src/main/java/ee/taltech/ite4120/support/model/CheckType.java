package ee.taltech.ite4120.support.model;

/** The three facts a benefit scheme's rules can depend on, each verified in one registry. */
public enum CheckType {
    RESIDENCE("Population Register · elukohtParing v1"),
    UNEMPLOYMENT("Töötukassa · tootuParing v1"),
    EMPLOYMENT("Employment Register · tootamineParing v1");

    private final String source;

    CheckType(String source) {
        this.source = source;
    }

    /** The registry and service an automatic check comes from — recorded as evidence. */
    public String source() {
        return source;
    }
}
