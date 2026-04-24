package codes.acegym.Objects;

/**
 * Model for PaymentPeriodTable.
 * period field stores the raw value the user typed (months).
 * days = months × 30 — that is what gets persisted in the DB.
 */
public class PaymentPeriod {

    private int    id;          // PaymentPeriodID
    private String name;        // PaymentPeriod  (e.g. "Monthly")
    private int    days;        // Days           (e.g. 30)

    /** Used when loading from DB (id already known). */
    public PaymentPeriod(int id, String name, int days) {
        this.id   = id;
        this.name = name;
        this.days = days;
    }

    // ── Getters ──────────────────────────────────────────────────
    public int    getId()   { return id;   }
    public String getName() { return name; }
    public int    getDays() { return days; }

    /** Convenience: months shown in the UI (days ÷ 30, rounded). */
    public int getMonths() { return Math.round(days / 30f); }
}