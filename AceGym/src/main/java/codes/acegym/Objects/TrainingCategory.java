package codes.acegym.Objects;

/**
 * Model for TrainingTypeTable.
 */
public class TrainingCategory {

    private int    id;           // TrainingTypeID
    private String categoryName; // TrainingCategory
    private double price;        // PriceOfCategory
    private double coachingFee;  // Coaching_Fee

    public TrainingCategory(int id, String categoryName, double price, double coachingFee) {
        this.id           = id;
        this.categoryName = categoryName;
        this.price        = price;
        this.coachingFee  = coachingFee;
    }

    // ── Getters ──────────────────────────────────────────────────
    public int    getId()           { return id;           }
    public String getCategoryName() { return categoryName; }
    public double getPrice()        { return price;        }
    public double getCoachingFee()  { return coachingFee;  }
}