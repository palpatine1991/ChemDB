package GString;

import javafx.util.Pair;
import jdk.nashorn.api.tree.Tree;
import org.openscience.cdk.graph.ConnectedComponents;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.*;

public class GString {
    public TreeNode root = new TreeNode();
    HashMap<String, IAtomContainer> db;
    int maxPathLength;
    String queryId = "query";

    public GString(HashMap<String, IAtomContainer> db, int maxPathLength) {
        this.db = db;
        this.maxPathLength = maxPathLength;
    }

    public void test(int depth) {
        this.test(depth, new LinkedList<>(), this.root);
    }

    public void buildIndex() {
        for (Map.Entry<String, IAtomContainer> entry : this.db.entrySet()) {
            this.processMolecule(entry.getKey(), entry.getValue(), this.db, this.root);
        }
    }

    public HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query) {
        HashMap<String, IAtomContainer> queryDB = new HashMap<>();
        HashMap<String, IAtomContainer> result = new HashMap<>();
        TreeNode queryRoot = new TreeNode();
        queryDB.put(this.queryId, query);

        this.processMolecule(this.queryId, query, queryDB, queryRoot);

        LinkedList<LinkedList<TreeNode>> leafNodesPaths = this.getLeafNodes(queryRoot);

        for (Map.Entry<String, IAtomContainer> entry : this.db.entrySet()) {
            if (this.isCandidate(leafNodesPaths, entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private boolean isCandidate(LinkedList<LinkedList<TreeNode>> leafNodesPaths, String id) {
        for (LinkedList<TreeNode> entry : leafNodesPaths) {
            if (!doesPathMatch(root, entry, id)) {
                return false;
            }
        }

        return true;
    }

    private boolean doesPathMatch(TreeNode root, LinkedList<TreeNode> path, String id) {
        if (path.size() == 0) {
            return true;
        }

        TreeNode queryNode = path.removeFirst();
        LinkedList<TreeNode> possibleChildren = getPossibleChildren(queryNode, root, id);

        for (TreeNode possibleChild : possibleChildren) {
            if (doesPathMatch(possibleChild, path, id)) {
                path.addFirst(queryNode);
                return true;
            }
        }

        path.addFirst(queryNode);
        return false;
    }

    private LinkedList<TreeNode> getPossibleChildren(TreeNode queryNode, TreeNode dbNode, String id) {
        LinkedList<TreeNode> result = new LinkedList<>();

        if (queryNode.type.equals(GStringNodeType.CYCLE)) {
            TreeNode candidate = dbNode.children.get(TreeNode.createSignature(queryNode.type, queryNode.size));
            if (candidate != null && doesNodeMatch(queryNode, candidate, id)) {
                result.add(candidate);
            }
        }
        else {
            LinkedList<TreeNode> candidates = dbNode.getChildrenByType(queryNode.type);

            for (TreeNode candidate : candidates) {
                if (doesNodeMatch(queryNode, candidate, id)) {
                    result.add(candidate);
                }
            }
        }

        return result;
    }

    private boolean doesNodeMatch(TreeNode query, TreeNode db, String id) {
        if (!db.matches.containsKey(id)) {
            return false;
        }

        if (db.type.equals(GStringNodeType.CYCLE)) {
            if (db.size != query.size) {
                return false;
            }
        }
        else {
            if (db.size < query.size) {
                return false;
            }
        }
        NodeMatchCount dbMatchCount = db.matches.getOrDefault(id, new NodeMatchCount());
        NodeMatchCount queryMatchCount = query.matches.getOrDefault(queryId, new NodeMatchCount());

        if (dbMatchCount.specialAtomCount < queryMatchCount.specialAtomCount) {
            return false;
        }
        if (dbMatchCount.specialBondCount < queryMatchCount.specialBondCount) {
            return false;
        }
        if (dbMatchCount.branchCount < queryMatchCount.branchCount) {
            return false;
        }
        return true;
    }

    private LinkedList<LinkedList<TreeNode>> getLeafNodes(TreeNode root) {
        LinkedList<LinkedList<TreeNode>> result = new LinkedList<>();
        for (TreeNode child : root.children.values()) {
            LinkedList<TreeNode> leafPath = new LinkedList<>();
            this.getLeafPaths(child, leafPath, result);
        }

        return result;
    }

    private void getLeafPaths(TreeNode root, LinkedList<TreeNode> leafPath, LinkedList<LinkedList<TreeNode>> result) {
        leafPath.addLast(root);
        if (root.children.size() == 0) {
            result.add((LinkedList<TreeNode>)leafPath.clone());
        }
        else {
            for (TreeNode child : root.children.values()) {
                this.getLeafPaths(child, leafPath, result);
            }
        }
        leafPath.removeLast();
    }

    private void test(int depth, LinkedList<String> records, TreeNode node) {
        if (node.size != 0) {
            records.push(node.type.id + "," + node.size);
        }
        if (records.size() == depth) {
            for (Iterator<String> i = records.descendingIterator(); i.hasNext();) {
                String record = i.next();
                System.out.println(record);
            }
            System.out.println(node.matches);
            for(NodeMatchCount match : node.matches.values()) {
                System.out.print("branch count: ");
                System.out.println(match.branchCount);
                System.out.print("atom count: ");
                System.out.println(match.specialAtomCount);
                System.out.print("bond count: ");
                System.out.println(match.specialBondCount);
            }
            System.out.println("---------------------------");
        }
        else {
            for (TreeNode child : node.children.values()) {
                this.test(depth, (LinkedList<String>)records.clone(), child);
            }
        }
    }

    private void processMolecule(String id, IAtomContainer molecule, HashMap<String, IAtomContainer> db, TreeNode root) {
        GStringGraph graph = new GStringGraph(molecule);

        for (GStringNode node : graph.nodes) {
            this.doDFS(node, id, db, root);
        }
    }

    private void doDFS(GStringNode node, String id, HashMap<String, IAtomContainer> db, TreeNode root) {
        this.doDFS(node, id, 0, root, new HashSet<>(), db);
    }

    private void doDFS(GStringNode node, String id, int depth, TreeNode parentNode, HashSet<GStringEdge> visitedEdges, HashMap<String, IAtomContainer> db) {
        depth++;

        TreeNode nextNode = parentNode.getChild(node);
        nextNode.updateMatch(id, node);

        if (depth < this.maxPathLength) {
            List<GStringEdge> edges = node.edges;

            for (GStringEdge edge : edges) {

                if (!visitedEdges.contains(edge)) {
                    visitedEdges.add(edge);

                    this.doDFS(edge.getOther(node), id, depth, nextNode, visitedEdges, db);

                    visitedEdges.remove(edge);
                }
            }
        }

    }
}
