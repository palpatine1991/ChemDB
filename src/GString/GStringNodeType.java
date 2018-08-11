package GString;

public enum GStringNodeType {
    CYCLE("Cycle"),
    STAR("Star"),
    PATH("Path"),
    TEMP("Temp");

    public String id;

    private GStringNodeType(String id) {
        this.id = id;
    }
}
