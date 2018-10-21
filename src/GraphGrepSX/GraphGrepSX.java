package GraphGrepSX;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.*;

public class GraphGrepSX {
    TreeNode root = new TreeNode("");
    HashMap<String, IAtomContainer> db;
    int maxPathLength;
    String queryId = "query";

    public GraphGrepSX(HashMap<String, IAtomContainer> db, int maxPathLength) {
        this.db = db;
        this.maxPathLength = maxPathLength;
    }

    public void buildIndex() {
        for (Map.Entry<String, IAtomContainer> entry : this.db.entrySet()) {
            this.processMolecule(entry.getKey(), entry.getValue(), this.db, this.root);
        }
    }

    public void test(int depth) {
        this.test(depth, new LinkedList<>(), this.root);
    }

    public HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query) {
        HashMap<String, IAtomContainer> queryDB = new HashMap<>();
        HashMap<String, IAtomContainer> result = new HashMap<>();
        TreeNode queryRoot = new TreeNode("");
        queryDB.put(this.queryId, query);

        this.processMolecule(this.queryId, query, queryDB, queryRoot);

        HashMap<LinkedList<String>, Integer> leafNodes = this.getLeafNodes(queryRoot);

        for (Map.Entry<String, IAtomContainer> entry : this.db.entrySet()) {
            if (this.isCandidate(leafNodes, entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private boolean isCandidate(HashMap<LinkedList<String>, Integer> leafNodes, String id) {
        boolean matches = true;

        for (Map.Entry<LinkedList<String>, Integer> entry : leafNodes.entrySet()) {
            TreeNode matchedLeaf = this.root.getNodeByPath((LinkedList<String>)entry.getKey().clone());
            if (matchedLeaf == null) {
                matches = false;
                break;
            }

            int matchedLeafCount = matchedLeaf.matches.getOrDefault(id, 0);
            if (matchedLeafCount < entry.getValue()) {
                matches = false;
                break;
            }
        }

        return matches;
    }

    private void test(int depth, LinkedList<String> records, TreeNode node) {
        if (!node.symbol.equals("")) {
            records.push(node.symbol);
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

    private HashMap<LinkedList<String>, Integer> getLeafNodes(TreeNode root) {
        HashMap<LinkedList<String>, Integer> result = new HashMap<>();
        for (TreeNode child : root.children.values()) {
            LinkedList<String> signature = new LinkedList<>();
            this.getLeafSignatures(child, signature, result);
        }

        return result;
    }

    private void getLeafSignatures(TreeNode root, LinkedList<String> signature, HashMap<LinkedList<String>, Integer> result) {
        signature.addLast(root.symbol);
        if (root.children.size() == 0) {
            result.put((LinkedList<String>)signature.clone(), root.matches.get(this.queryId));
        }
        else {
            for (TreeNode child : root.children.values()) {
                this.getLeafSignatures(child, signature, result);
            }
        }
        signature.removeLast();
    }

    private void processMolecule(String id, IAtomContainer molecule, HashMap<String, IAtomContainer> db, TreeNode root) {
        for (IAtom atom : molecule.atoms()) {
            this.doDFS(atom, id, db, root);
        }
    }

    private void doDFS(IAtom atom, String id, HashMap<String, IAtomContainer> db, TreeNode root) {
        this.doDFS(atom, id, 0, root, new HashSet<>(), db);
    }

    private void doDFS(IAtom atom, String id, int depth, TreeNode parentNode, HashSet<IBond> visitedBonds, HashMap<String, IAtomContainer> db) {
        depth++;

        TreeNode atomNode = parentNode.getChild(atom.getSymbol());
        atomNode.incrementMatch(id);

        if (depth < this.maxPathLength) {
            List<IBond> bonds = db.get(id).getConnectedBondsList(atom);

            for (IBond bond : bonds) {
                if (!visitedBonds.contains(bond)) {
                    visitedBonds.add(bond);

                    TreeNode bondNode;

                    if (bond.isAromatic()) {
                        bondNode = atomNode.getChild("AROMATIC");
                    }
                    else {
                        bondNode = atomNode.getChild(bond.getOrder().name());
                    }

                    this.doDFS(bond.getOther(atom), id, depth, bondNode, visitedBonds, db);

                    visitedBonds.remove(bond);
                }
            }
        }
    }
}
