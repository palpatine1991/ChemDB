package GString;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.*;

public class GString {
    TreeNode root = new TreeNode();
    HashMap<String, IAtomContainer> db;
    int maxPathLength;

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
