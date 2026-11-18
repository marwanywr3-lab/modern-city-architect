package moderncity.road;

public enum RoadClass {
    HIGHWAY("Highway (3 Lanes/Dir)", 11, 1, 2),
    AVENUE("Avenue (2 Lanes/Dir)", 7, 1, 3),
    LOCAL("Local Street (1 Lane/Dir)", 4, 0, 2);

    public final String displayName;
    public final int halfWidth;
    public final int medianHalfWidth;
    public final int sidewalkWidth;

    RoadClass(String displayName, int halfWidth, int medianHalfWidth, int sidewalkWidth) {
        this.displayName = displayName;
        this.halfWidth = halfWidth;
        this.medianHalfWidth = medianHalfWidth;
        this.sidewalkWidth = sidewalkWidth;
    }

    public RoadClass next() {
        RoadClass[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
