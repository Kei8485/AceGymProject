package codes.acegym.Objects;

/**
 * Holds the result of an upgrade eligibility check.
 * Passed from PaymentDAO → PaymentController to drive UI decisions.
 */
public class UpgradeResult {

    public enum Status {
        /** No active plan — proceed as a fresh payment. */
        NO_ACTIVE_PLAN,
        /** Active plan found, new plan costs more — collect the difference. */
        UPGRADE_ALLOWED,
        /** New plan costs less — block until current period expires. */
        DOWNGRADE_BLOCKED,
        /** Same plan already active — block. */
        SAME_PLAN_BLOCKED
    }

    private final Status status;

    /** Current active plan price (per period), 0 when NO_ACTIVE_PLAN. */
    private final double currentPrice;

    /** New plan price (per period). */
    private final double newPrice;

    /** Difference to charge (newPrice − currentPrice). 0 when not UPGRADE_ALLOWED. */
    private final double topUpAmount;

    /** Human-readable expiry date of the current plan (e.g. "May 15, 2026"). */
    private final String currentPlanExpiry;

    /** TrainingCategory of the current active plan. */
    private final String currentCategory;

    /** PaymentPeriod name of the current active plan. */
    private final String currentPeriod;

    public UpgradeResult(Status status,
                         double currentPrice,
                         double newPrice,
                         double topUpAmount,
                         String currentPlanExpiry,
                         String currentCategory,
                         String currentPeriod) {
        this.status            = status;
        this.currentPrice      = currentPrice;
        this.newPrice          = newPrice;
        this.topUpAmount       = topUpAmount;
        this.currentPlanExpiry = currentPlanExpiry;
        this.currentCategory   = currentCategory;
        this.currentPeriod     = currentPeriod;
    }

    // ── Factory helpers ──────────────────────────────────────────────────────

    public static UpgradeResult noActivePlan() {
        return new UpgradeResult(Status.NO_ACTIVE_PLAN, 0, 0, 0, null, null, null);
    }

    public static UpgradeResult upgradeAllowed(double currentPrice, double newPrice,
                                               String expiry, String category, String period) {
        return new UpgradeResult(Status.UPGRADE_ALLOWED,
                currentPrice, newPrice, newPrice - currentPrice,
                expiry, category, period);
    }

    public static UpgradeResult downgradeBlocked(double currentPrice, double newPrice,
                                                 String expiry, String category, String period) {
        return new UpgradeResult(Status.DOWNGRADE_BLOCKED,
                currentPrice, newPrice, 0, expiry, category, period);
    }

    public static UpgradeResult samePlanBlocked(double price, String expiry,
                                                String category, String period) {
        return new UpgradeResult(Status.SAME_PLAN_BLOCKED,
                price, price, 0, expiry, category, period);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Status getStatus()            { return status; }
    public double getCurrentPrice()      { return currentPrice; }
    public double getNewPrice()          { return newPrice; }
    public double getTopUpAmount()       { return topUpAmount; }
    public String getCurrentPlanExpiry() { return currentPlanExpiry; }
    public String getCurrentCategory()   { return currentCategory; }
    public String getCurrentPeriod()     { return currentPeriod; }

    public boolean isBlocked() {
        return status == Status.DOWNGRADE_BLOCKED || status == Status.SAME_PLAN_BLOCKED;
    }
}