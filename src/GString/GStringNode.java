package GString;

import java.util.LinkedList;

public class GStringNode {
    GStringNodeType type;
    int specialAtomCount;
    int specialBondCount;
    int branchCount;
    int size;

    LinkedList<GStringEdge> edges = new LinkedList<>();

    public GStringNode(GStringNodeType type, int size, int specialAtomCount, int specialBondCount, int branchCount) {
        this.type = type;
        this.size = size;
        this.specialAtomCount = specialAtomCount;
        this.specialBondCount = specialBondCount;
        this.branchCount = branchCount;
    }
}
