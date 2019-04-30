package GString;

import javafx.util.Pair;
import jdk.nashorn.api.tree.Tree;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TreeNode {
    public static int count = 0;

    Map<String, NodeMatchCount> matches = new HashMap<>();
    Map<String, TreeNode> children = new HashMap<>();
    int size;
    GStringNodeType type;


    public TreeNode() {TreeNode.count++;}
    public TreeNode(GStringNode origin) {
        this.size = origin.size;
        this.type = origin.type;
        TreeNode.count++;
    }

    public TreeNode getChild(GStringNode origin) {
        String signature = TreeNode.getSignature(origin);
        if (!this.children.containsKey(signature)) {
            this.children.put(signature, new TreeNode(origin));
        }

        return this.children.get(signature);
    }

    public LinkedList<TreeNode> getChildrenByType(GStringNodeType type) {
        LinkedList<TreeNode> result = new LinkedList<>();

        for (TreeNode child : children.values()) {
            if (child.type.equals(type)) {
                result.push(child);
            }
        }

        return result;
    }

    public void updateMatch(String id, GStringNode node) {
        NodeMatchCount oldNodeMatchCounts = this.matches.getOrDefault(id, new NodeMatchCount(0, 0, 0));

        this.matches.put(
            id,
            new NodeMatchCount(
                    Math.max(node.specialAtomCount, oldNodeMatchCounts.specialAtomCount),
                    Math.max(node.specialBondCount, oldNodeMatchCounts.specialBondCount),
                    Math.max(node.branchCount, oldNodeMatchCounts.branchCount)
            )
        );
    }

    private static String getSignature(GStringNode node) {
        return TreeNode.createSignature(node.type, node.size);
    }

    public static String getSignature(TreeNode node) {
        return TreeNode.createSignature(node.type, node.size);
    }

    public static String createSignature(GStringNodeType type, int size) {
        return type.id + "," + size;
    }

    public static Pair<GStringNodeType, Integer> parseSignature(String signature) {
        String[] words = signature.split(",");

        int size = Integer.parseInt(words[1]);
        GStringNodeType type = null;

        if (words[0].equals(GStringNodeType.CYCLE.id)) {
            type = GStringNodeType.CYCLE;
        }
        else if (words[0].equals(GStringNodeType.PATH.id)) {
            type = GStringNodeType.PATH;
        }
        else if (words[0].equals(GStringNodeType.STAR.id)) {
            type = GStringNodeType.STAR;
        }
        else {
            assert false : "Invalid entry of node signature";
        }

        return new Pair<GStringNodeType, Integer>(type, size);
    }

}
