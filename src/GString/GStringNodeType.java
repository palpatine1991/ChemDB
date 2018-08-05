package GString;

public enum GStringNodeType {
    CYCLE("Cycle"),
    STAR("Star"),
    PATH("Path"),
    BASIC("Basic"),
    TEMP("Temp");

    public String id;

    private GStringNodeType(String id) {
        this.id = id;
    }
}
