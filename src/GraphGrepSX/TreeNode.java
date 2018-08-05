package GraphGrepSX;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TreeNode {
    Map<String, Integer> matches = new HashMap<>();
    Map<String, TreeNode> children = new HashMap<>();
    String symbol;

    public TreeNode(String symbol) {
        this.symbol = symbol;
    }

    public TreeNode getChild(String symbol) {
        if (!this.children.containsKey(symbol)) {
            this.children.put(symbol, new TreeNode(symbol));
        }

        return this.children.get(symbol);
    }

    public void incrementMatch(String id) {
        this.matches.put(id, this.matches.getOrDefault(id, 0) + 1);
    }

    public TreeNode getNodeByPath(LinkedList<String> path) {
        if (path.size() == 0) {
            return this;
        }

        if (this.children.containsKey(path.getFirst())) {
            TreeNode matchedChild = this.children.get(path.getFirst());
            path.removeFirst();
            return matchedChild.getNodeByPath(path);
        }
        else {
            return null;
        }
    }
}
