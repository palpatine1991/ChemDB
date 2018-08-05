package GString;

import java.util.Iterator;
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

    public boolean isNeighbour(GStringNode node) {
        for (Iterator<GStringEdge> i = edges.iterator(); i.hasNext();) {
            GStringEdge edge = i.next();
            if (edge.doesContainNode(node)) {
                return true;
            }
        }

        return false;
    }

    public boolean isNeighbour(LinkedList<GStringNode> list) {
        for (Iterator<GStringNode> i = list.iterator(); i.hasNext();) {
            GStringNode node = i.next();
            if (isNeighbour(node)) {
                return true;
            }
        }

        return false;
    }
}
