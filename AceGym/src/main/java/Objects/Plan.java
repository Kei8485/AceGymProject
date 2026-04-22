package Objects;

public class Plan {
    private String name;
    private int period;

    public Plan(String name, int period) {
        this.name = name;
        this.period = period;
    }

    public String getName() { return name; }

    public Integer getPeriod() { return period; } // boxed Integer, not int
}