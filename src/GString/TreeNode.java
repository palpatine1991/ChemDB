package GString;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TreeNode {
    Map<String, NodeMatchCount> matches = new HashMap<>();
    Map<String, TreeNode> children = new HashMap<>();
    int size;
    GStringNodeType type;


    public TreeNode() {}
    public TreeNode(GStringNode origin) {
        this.size = origin.size;
        this.type = origin.type;
    }

    public TreeNode getChild(GStringNode origin) {
        String signature = TreeNode.getSignature(origin);
        if (!this.children.containsKey(signature)) {
            this.children.put(signature, new TreeNode(origin));
        }

        return this.children.get(signature);
    }

    public void updateMatch(String id, GStringNode node) {
        NodeMatchCount oldNodeMatchCounts = this.matches.getOrDefault(id, new NodeMatchCount(0, 0, 0));

        this.matches.put(id, this.matches.getOrDefault(
                id,
                new NodeMatchCount(
                        Math.max(node.specialAtomCount, oldNodeMatchCounts.specialAtomCount),
                        Math.max(node.specialBondCount, oldNodeMatchCounts.specialBondCount),
                        Math.max(node.branchCount, oldNodeMatchCounts.branchCount)
                )
        ));
    }

    public TreeNode getNodeByPath(LinkedList<GStringNode> path) {
        if (path.size() == 0) {
            return this;
        }

        String firstNodeSignature = TreeNode.getSignature(path.getFirst());

        if (this.children.containsKey(firstNodeSignature)) {
            TreeNode matchedChild = this.children.get(firstNodeSignature);
            path.removeFirst();
            return matchedChild.getNodeByPath(path);
        }
        else {
            return null;
        }
    }

    private static String getSignature(GStringNode node) {
        return node.type.id + "," + node.size;
    }
}
