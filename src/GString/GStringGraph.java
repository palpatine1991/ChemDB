package GString;

import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.Intractable;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;

import java.util.*;

class NewNodeEdgeResult {
    public GStringNode newNode;
    public GStringEdge newEdge;

    public NewNodeEdgeResult(GStringNode newNode, GStringEdge newEdge) {
        this.newEdge = newEdge;
        this.newNode = newNode;
    }
}

public class GStringGraph {
    private static CycleFinder cycleFinder = Cycles.mcb();

    public LinkedList<GStringNode> nodes = new LinkedList<>();

    private HashMap<IAtom, LinkedList<GStringNode>> processedAtoms = new HashMap<>();
    private HashMap<IAtomContainer, GStringNode> processedRings = new HashMap<>();
    private IAtomContainer molecule;
    private IRingSet completeRingSet;
    private int starFanoutTreshold = 3;

    public GStringGraph(IAtomContainer molecule) {
        this.molecule = molecule;

        try {
            Cycles cycles = cycleFinder.find(molecule, 10);
            completeRingSet = cycles.toRingSet();
        }
        catch (Intractable e) {
            // ignore error - MCB should never be intractable
        }

        for (IBond bond : molecule.bonds()) {
            bond.setIsInRing(false);
        }

        createGraphWithCycles();
        findAndProcessStars();
        findAndProcessPaths();
        //TODO process branches
    }

    private void findAndProcessPaths() {
        LinkedList<GStringNode> processedNodes = new LinkedList<>();

        for (GStringNode node : nodes) {
            if (!node.type.equals(GStringNodeType.TEMP)) {
                processedNodes.add(node);
            }
        }

        //if there are no special nodes -> only 1 path exists.
        if (processedNodes.size() == 0) {
            //find ending one
            for (GStringNode node : nodes) {
                if (node.edges.size() == 1) {
                    node.type = GStringNodeType.PATH;
                    node.size = 1;
                    processPathNode(node, node.edges.getFirst().getOther(node));
                }
            }

        }

        for (GStringNode processedNode : processedNodes) {
            processAdjacentPaths(processedNode);
        }
    }

    private void processAdjacentPaths(GStringNode node) {
        ListIterator<GStringEdge> iterator = node.edges.listIterator();

        while (iterator.hasNext()) {
            GStringEdge startingEdge = iterator.next();

            GStringNode otherNode = startingEdge.getOther(node);

            //paths can consist only of TEMP nodes
            if (!otherNode.type.equals(GStringNodeType.TEMP)) {
                continue;
            }

            //not a path, just a branch
            if (otherNode.edges.size() == 1) {
                continue;
            }

            otherNode.type = GStringNodeType.PATH;
            otherNode.size = 1;

            //If the node is connecting 2 or more already processed nodes,
            //we want to just connect them by path of size 1 and process its TEMP neighbours
            if (getNonTempNeighboursCount(otherNode) > 1) {
                processAdjacentPaths(otherNode);
                return;
            }

            LinkedList<GStringNode> nextPathNodes = getOtherTempNeighbours(otherNode, node);

            switch(nextPathNodes.size()) {
                case 0:
                    //we are done
                    break;
                case 1:
                    processPathNode(otherNode, nextPathNodes.getFirst());
                    break;
                case 2:
                    NewNodeEdgeResult splitResult = splitNode(otherNode, nextPathNodes.getFirst());
                    processPathNode(otherNode, nextPathNodes.getFirst());

                    //There is a cycle bigger then threshold. Whole cycle is consumed as one path.
                    //We need to disconnect the cycle on one end and treat it as a path
                    if (!nodes.contains(nextPathNodes.getLast())) {
                        nodes.remove(splitResult.newNode);
                        for (GStringEdge edge : splitResult.newNode.edges) {
                            edge.getOther(splitResult.newNode).edges.remove(edge);
                        }
                    }
                    else {
                        iterator.add(splitResult.newEdge);
                        processPathNode(splitResult.newNode, nextPathNodes.getLast());
                    }

                    break;
                default:
                    assert false : "Temp node has 3 temp neighbours!";
            }
        }
    }

    //Duplicates the node and each new node will have only 1 TEMP neighbour. Returns new node and edge
    //originNode should be PATH node with exactly 2 TEMP neighbours and 1 non-TEMP neighbour
    private NewNodeEdgeResult splitNode(GStringNode originNode, GStringNode originNodeContinuation) {
        assert originNode.edges.size() == 3;
        assert originNode.type.equals(GStringNodeType.PATH);
        assert originNode.size == 1;
        assert originNodeContinuation.type.equals(GStringNodeType.TEMP);

        GStringNode newNode = new GStringNode(
            GStringNodeType.PATH,
            1,
            originNode.specialAtomCount,
            originNode.specialBondCount,
            0
        );

        nodes.add(newNode);

        GStringEdge toBeRemovedEdge = null;
        GStringEdge duplicateEdge = null;

        boolean continuationVisited = false;
        boolean nonTempVisited = false;
        boolean otherTempVisited = false;

        for (GStringEdge edge : originNode.edges) {
            GStringNode otherNode = edge.getOther(originNode);
            if (otherNode.equals(originNodeContinuation)) {
                //nothing, this edge should be unchanged

                continuationVisited = true;
            }
            else if (!otherNode.type.equals(GStringNodeType.TEMP)) {
                //we need to duplicate this edge to new node
                duplicateEdge = new GStringEdge(edge.isSpecial, otherNode, newNode);
                newNode.edges.add(duplicateEdge);

                nonTempVisited = true;
            }
            else {
                edge.reconnectEdge(originNode, newNode);
                newNode.edges.add(edge);
                toBeRemovedEdge = edge;

                otherTempVisited = true;
            }
        }

        assert continuationVisited;
        assert nonTempVisited;
        assert otherTempVisited;

        originNode.edges.remove(toBeRemovedEdge);

        return new NewNodeEdgeResult(newNode, duplicateEdge);
    }

    private void processPathNode(GStringNode pathNode, GStringNode nextNode) {
        if (nextNode == null) {
            return;
        }

        GStringNode pathContinuation = getOtherTempNeighbour(nextNode, pathNode);
        joinNodeToPath(pathNode, nextNode);
        processPathNode(pathNode, pathContinuation);
    }

    private int getNonTempNeighboursCount(GStringNode node) {
        int result = 0;

        for (GStringEdge edge : node.edges) {
            if (!edge.getOther(node).type.equals(GStringNodeType.TEMP)) {
                result++;
            }
        }

        return result;
    }

    private LinkedList<GStringNode> getOtherTempNeighbours(GStringNode node, GStringNode badNeighbour) {
        LinkedList<GStringNode> result = new LinkedList<>();

        for (GStringEdge edge : node.edges) {
            if (!edge.getOther(node).equals(badNeighbour) && edge.getOther(node).type.equals(GStringNodeType.TEMP)) {
                result.add(edge.getOther(node));
            }
        }

        return result;
    }

    private GStringNode getOtherTempNeighbour(GStringNode node, GStringNode badNeighbour) {
        GStringNode result = null;

        for (GStringEdge edge : node.edges) {
            if (!edge.getOther(node).equals(badNeighbour) && edge.getOther(node).type.equals(GStringNodeType.TEMP)) {
                if (result != null) {
                    System.err.println("Next path node has more than one TEMP neighbours!");
                }
                result = edge.getOther(node);
            }
        }

        return result;
    }

    private void joinNodeToPath(GStringNode pathNode, GStringNode nodeToJoin) {
        for (GStringEdge edge : nodeToJoin.edges) {
            if (edge.getOther(nodeToJoin).equals(pathNode)) {
                pathNode.edges.remove(edge);
                if (edge.isSpecial) {
                    pathNode.specialBondCount++;
                }
            }
            else {
                edge.reconnectEdge(nodeToJoin, pathNode);
                pathNode.edges.add(edge);
            }
        }

        pathNode.size++;
        pathNode.specialAtomCount += nodeToJoin.specialAtomCount;

        nodes.remove(nodeToJoin);
    }

    private void findAndMarkStars() {
        //find and create stars. not deleting fanout nodes, yet
        for (GStringNode node : nodes) {
            if (node.type.equals(GStringNodeType.TEMP) && node.edges.size() >= starFanoutTreshold) {
                int starSize = node.edges.size();
                int specialAtomsCount = node.specialAtomCount;
                int specialBondCount = 0;
                for (GStringEdge edge : node.edges) {
                    GStringNode otherNode = edge.getOther(node);
                    if (otherNode.type.equals(GStringNodeType.CYCLE)) {
                        starSize--;
                    }
                    else {
                        if (edge.isSpecial) {
                            specialBondCount++;
                        }
                        if (otherNode.isOriginallySpecial) {
                            specialAtomsCount++;
                        }
                    }
                }

                if (starSize >= starFanoutTreshold) {
                    node.type = GStringNodeType.STAR;
                    node.specialAtomCount = specialAtomsCount;
                    node.specialBondCount = specialBondCount;
                    node.size = starSize;
                }
            }
        }
    }

    private void processStarEdges() {
        LinkedList<GStringNode> nodesToBeRemoved = new LinkedList<>();

        //remove star fanout nodes
        for (GStringNode centerNode : nodes) {
            if (!centerNode.type.equals(GStringNodeType.STAR)) {
                continue;
            }

            LinkedList<GStringEdge> edgesToBeRemoved = new LinkedList<>();
            LinkedList<GStringEdge> edgesToBeAdded = new LinkedList<>();

            for (GStringEdge starEdge : centerNode.edges) {
                GStringNode fanoutNode = starEdge.getOther(centerNode);

                //We only care about temp nodes
                if (!fanoutNode.type.equals(GStringNodeType.TEMP)) {
                    continue;
                }

                //reconnect all edges from fanout node to the center
                for (GStringEdge fanoutEdge : fanoutNode.edges) {
                    //fanout edge should be deleted
                    if (fanoutEdge.doesContainNode(centerNode)) {
                        edgesToBeRemoved.add(fanoutEdge);
                        continue;
                    }

                    fanoutEdge.reconnectEdge(fanoutNode, centerNode);
                    edgesToBeAdded.add(fanoutEdge);
                }

                nodesToBeRemoved.add(fanoutNode);
            }

            centerNode.edges.removeAll(edgesToBeRemoved);
            centerNode.edges.addAll(edgesToBeAdded);
        }

        nodes.removeAll(nodesToBeRemoved);
    }

    private void findAndProcessStars() {
        findAndMarkStars();
        processStarEdges();
    }

    private void createGraphWithCycles() {
        completeRingSet.atomContainers().forEach((ring) -> {
            processRing(ring);
        });

        molecule.atoms().forEach((atom) -> {
            processAtom(atom);
        });
    }

    private void processAtom(IAtom atom) {
        LinkedList<GStringNode> linkedNodes;

        if (!processedAtoms.containsKey(atom)) {
            int nonDefaultCount = (isDefaultAtom(atom)) ? 0 : 1;

            GStringNode node = new GStringNode(GStringNodeType.TEMP, 1, nonDefaultCount, 0, 0);
            node.isOriginallySpecial = nonDefaultCount == 1;
            nodes.add(node);
            linkedNodes = new LinkedList<GStringNode>();
            linkedNodes.add(node);
        }
        else {
            linkedNodes = processedAtoms.get(atom);
        }

        molecule.getConnectedBondsList(atom).forEach((bond) -> {
            //ring bonds are already processed
            if (bond.isInRing()) {
                return;
            }
            IAtom otherAtom = bond.getOther(atom);
            //Only connect already processed neighbours.
            //Others will be connected when they will be processed
            linkedNodes.forEach((node) -> {
                if (processedAtoms.containsKey(otherAtom) && !node.isNeighbour(processedAtoms.get(otherAtom))) {
                    processedAtoms.get(otherAtom).forEach((otherNode) -> {
                        GStringEdge edge = new GStringEdge(!isDefaultBond(bond), node, otherNode);
                        edge.registerEdgeToNodes();
                    });
                }
            });
        });

        processedAtoms.put(atom, linkedNodes);
    }

    private void processRing(IAtomContainer ring) {
        if (processedRings.containsKey(ring)) {
            return;
        }

        GStringNode node = new GStringNode(GStringNodeType.CYCLE, ring.getAtomCount(), 0, 0, 0);
        nodes.add(node);

        processedRings.put(ring, node);

        node.specialBondCount = getRingSpecialBondCount(ring);

        //setting special atom count and finding neighbours
        int specialAtomCount = 0;
        for (Iterator<IAtom> i = ring.atoms().iterator(); i.hasNext();) {
            IAtom atom = i.next();
            if (!isDefaultAtom(atom)) {
                specialAtomCount++;
            }

            //registering new cycle node to all present atoms
            if (processedAtoms.containsValue(atom)) {
                processedAtoms.get(atom).add(node);
            }
            else {
                LinkedList<GStringNode> relations = new LinkedList<GStringNode>();
                relations.add(node);
                processedAtoms.put(atom, relations);
            }

            createCycleEdgesForAtom(atom, ring, node);
        }
        node.specialAtomCount = specialAtomCount;

    }

    /**
     * Creates and registers GStringEdges from the processed ring to already processed connected rings
     * In this context connected means sharing atom. Rings which are connected via bond are processed later on.
     * @param atom atom in ring being processed
     * @param ring processed ring containing the atom
     * @param cycleNode GString node belonging to processed ring
     */
    private void createCycleEdgesForAtom(IAtom atom, IAtomContainer ring, GStringNode cycleNode) {
        IRingSet atomRings = completeRingSet.getRings(atom);
        atomRings.atomContainers().forEach((ringContainer) -> {
            if (ringContainer == ring) {
                return;
            }

            if (processedRings.containsKey(ringContainer) && !cycleNode.isNeighbour(processedRings.get(ringContainer))) {
                GStringEdge edge = new GStringEdge(false, cycleNode, processedRings.get(ringContainer));
                edge.registerEdgeToNodes();
            }
        });
    }

    /**
     * Also sets the isInRing flag
     * @param ring
     * @return
     */
    private int getRingSpecialBondCount(IAtomContainer ring) {
        int specialBondCount = 0;

        //setting special bond count
        for (Iterator<IBond> i = ring.bonds().iterator(); i.hasNext();) {
            IBond bond = i.next();

            bond.setIsInRing(true);
            if (!isDefaultBond(bond)) {
                specialBondCount++;
            }
        }

        return specialBondCount;
    }

    private boolean isDefaultAtom(IAtom atom) {
        return atom.getSymbol().equals("C");
    }

    private boolean isDefaultBond(IBond bond) {
        return bond.getOrder().equals(Bond.Order.SINGLE);
    }
}
