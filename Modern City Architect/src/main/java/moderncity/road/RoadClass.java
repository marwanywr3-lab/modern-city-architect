package moderncity.road;

public enum RoadClass {
    SMALL_5("Small Street (5b)", 5, 2),
    MEDIUM_7("Medium Avenue (7b)", 7, 2),
    LARGE_12("Large Boulevard (12b)", 12, 3),
    HIGHWAY_8("Expressway (8b)", 8, 2),
    HIGHWAY_16("Mega Highway (16b)", 16, 3);

    public final String displayName;
    public final int roadWidth;
    public final int sidewalkWidth;

    RoadClass(String displayName, int roadWidth, int sidewalkWidth) {
        this.displayName = displayName;
        this.roadWidth = roadWidth;
        this.sidewalkWidth = sidewalkWidth;
    }

    public boolean isHighway() {
        return this == HIGHWAY_8 || this == HIGHWAY_16;
    }

    public RoadClass next() {
        RoadClass[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
