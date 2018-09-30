package GString;

public class GStringDuplicityCheck {
    public GStringNode otherNode;
    public NodeMatchCount counts;
    public int size;

    public GStringDuplicityCheck(GStringNode otherNode, NodeMatchCount counts, int size) {
        this.otherNode = otherNode;
        this.counts = counts;
        this.size = size;
    }
}
