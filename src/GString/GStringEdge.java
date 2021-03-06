package GString;

import java.util.HashSet;

public class GStringEdge {


    public boolean isSpecial;
    GStringNode node1;
    GStringNode node2;

    public GStringEdge(boolean isSpecial, GStringNode node1, GStringNode node2) {
        this.isSpecial = isSpecial;
        this.node1 = node1;
        this.node2 = node2;
    }

    public void registerEdgeToNodes() {
        node1.edges.add(this);
        node2.edges.add(this);
    }

    public boolean doesContainNode(GStringNode node) {
        return (node == this.node1 || node == this.node2);
    }

    public GStringNode getOther(GStringNode node) {
        if (node == this.node1) {
            return this.node2;
        } else if (node == this.node2) {
            return this.node1;
        } else {
            assert false : "Asking edge for 'other node' when specified node is it present in edge";
            return null;
        }
    }

    public void reconnectEdge(GStringNode originalNode, GStringNode newNode) {
        if (originalNode.equals(node1)) {
            node1 = newNode;
        }
        else if (originalNode.equals(node2)) {
            node2 = newNode;
        }
        else {
            assert false : "Replacing node in edge where it is not registered!";
        }
    }
}
