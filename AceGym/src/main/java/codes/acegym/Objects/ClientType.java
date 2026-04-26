package codes.acegym.Objects;

/**
 * Model for ClientTypeTable.
 * Client types are fixed (Non Member / Member); Discount and MembershipFee are editable.
 */
public class ClientType {

    private int    id;            // ClientTypeID
    private String typeName;      // ClientType    (e.g. "Member")
    private double membershipFee; // MembershipFee (e.g. 1000.00)
    private String discount;      // Discount      (e.g. "40%")

    /** Legacy constructor — membershipFee defaults to 0. */
    public ClientType(int id, String typeName, String discount) {
        this(id, typeName, 0.0, discount);
    }

    /** Full constructor used when loading from DB. */
    public ClientType(int id, String typeName, double membershipFee, String discount) {
        this.id            = id;
        this.typeName      = typeName;
        this.membershipFee = membershipFee;
        this.discount      = discount;
    }

    // ── Getters ──────────────────────────────────────────────────
    public int    getId()            { return id;            }
    public String getTypeName()      { return typeName;      }
    public double getMembershipFee() { return membershipFee; }
    public String getDiscount()      { return discount;      }
}