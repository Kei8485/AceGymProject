package codes.acegym.Objects;

/**
 * Model for ClientTypeTable.
 * Client types are fixed (Non Member / Member); only Discount is editable.
 */
public class ClientType {

    private int    id;         // ClientTypeID
    private String typeName;   // ClientType  (e.g. "Member")
    private String discount;   // Discount    (e.g. "40%")

    public ClientType(int id, String typeName, String discount) {
        this.id       = id;
        this.typeName = typeName;
        this.discount = discount;
    }

    // ── Getters ──────────────────────────────────────────────────
    public int    getId()       { return id;       }
    public String getTypeName() { return typeName; }
    public String getDiscount() { return discount; }
}