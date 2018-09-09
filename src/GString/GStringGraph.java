package GString;

import javafx.util.Pair;
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
    private int starFanoutTreshold = 4;

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
        processBranches();
    }

    private void processBranches() {
        ListIterator<GStringNode> iterator = nodes.listIterator();

        while (iterator.hasNext()) {
            GStringNode node = iterator.next();

            if (!node.type.equals(GStringNodeType.TEMP)) {
                continue;
            }

            assert node.edges.size() == 1;

            GStringEdge connectedEdge = node.edges.getFirst();
            GStringNode otherNode = connectedEdge.getOther(node);

            otherNode.branchCount++;
            otherNode.specialAtomCount += node.specialAtomCount;
            if (connectedEdge.isSpecial) {
                otherNode.specialBondCount++;
            }
            otherNode.edges.remove(connectedEdge);
            iterator.remove();
        }
    }

    private void findAndProcessPaths() {
        LinkedList<GStringNode> processedNodes = new LinkedList<>();

        for (GStringNode node : nodes) {
            if (!node.type.equals(GStringNodeType.TEMP)) {
                processedNodes.add(node);
            }
        }

        //if there are no special nodes -> only 1 path exists.
        //TODO: more paths can exist if STAR fanout is bigger than 3
        if (processedNodes.size() == 0) {
            //find ending one
            boolean loneLongCycle = true;
            for (GStringNode node : nodes) {
                if (node.edges.size() == 1) {
                    loneLongCycle = false;
                    node.type = GStringNodeType.PATH;
                    node.size = 1;
                    processPathNode(node, node.edges.getFirst().getOther(node));
                }
            }

            //if the whole molecule is a single atom, we want to transofrm it to the path of the size 1
            if (loneLongCycle && nodes.size() == 1 && nodes.getFirst().type.equals(GStringNodeType.TEMP)) {
                GStringNode lonelyNode = nodes.getFirst();
                lonelyNode.type = GStringNodeType.PATH;
                lonelyNode.size = 1;
            }
            //Molecule is just a big cycle (bigger than threshold)
            //We want to make it a big path
            else if (loneLongCycle) {
                GStringNode startingNode = nodes.getFirst();
                startingNode.type = GStringNodeType.PATH;
                startingNode.size = 1;
                processPathNode(startingNode, startingNode.edges.getFirst().getOther(startingNode));
            }

        }

        for (GStringNode processedNode : processedNodes) {
            processAdjacentPaths(processedNode);
        }
    }

    private void processAdjacentPaths(GStringNode node) {
        ListIterator<GStringEdge> iterator = node.edges.listIterator();
        HashSet<GStringNode> nodesToRemove = new HashSet<>();

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

            LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> pathResults = getPaths(node, otherNode, nodesToRemove);

            boolean edgeRemoved = false;

            for (Pair<LinkedList<GStringEdge>, NodeMatchCount> result : pathResults) {
                GStringEdge lastEdge = result.getKey().getLast();
                NodeMatchCount count = result.getValue();
                GStringNode endNode = null;
                int pathSize = result.getKey().size();

                if (!lastEdge.node1.type.equals(GStringNodeType.TEMP)) {
                    endNode = lastEdge.node1;
                }
                else if (!lastEdge.node2.type.equals(GStringNodeType.TEMP)) {
                    endNode = lastEdge.node2;
                }
                //blind end -> do nothing but we need to increase the size of path
                else {
                    pathSize++;
                }

                GStringNode pathNode = new GStringNode(GStringNodeType.PATH, pathSize, count.specialAtomCount, count.specialBondCount, count.branchCount);
                nodes.add(pathNode);

                if (!edgeRemoved) {
                    iterator.remove();
                    edgeRemoved = true;
                }
                GStringEdge newEdge1 = new GStringEdge(startingEdge.isSpecial, pathNode, node);
                iterator.add(newEdge1);
                pathNode.edges.add(newEdge1);

                if (endNode != null) {
                    endNode.edges.remove(lastEdge);
                    GStringEdge newEdge2 = new GStringEdge(lastEdge.isSpecial, pathNode, endNode);
                    newEdge2.registerEdgeToNodes();
                }
            }

            //region old
            /*otherNode.type = GStringNodeType.PATH;
            otherNode.size = 1;

            LinkedList<GStringNode> nextPathNodes = getOtherTempNeighbours(otherNode, node);


            //TODO:might be more than two
            //find out whether nextPathNode is not a branch. If so, remove it from the list of potential continuations
            if (nextPathNodes.size() == 2) {
                GStringNode firstNode = nextPathNodes.getFirst();
                GStringNode secondNode = nextPathNodes.getLast();

                if (firstNode.edges.size() == 1) {
                    nextPathNodes.remove(firstNode);
                }
                else if (secondNode.edges.size() == 1) {
                    nextPathNodes.remove(secondNode);
                }
            }

            switch(nextPathNodes.size()) {
                case 0:
                    //we are done
                    break;
                case 1:
                    processPathNode(otherNode, nextPathNodes.getFirst());
                    break;
                case 2:
                    //TODO: might be in different cases than 2
                    NewNodeEdgeResult splitResult = splitNode(otherNode, nextPathNodes.getFirst(), node);
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
                    //TODO: might be more than two
                    assert false : "Temp node has 3 temp neighbours!";
            }*/
            //endregion
        }

        nodes.removeAll(nodesToRemove);
    }

    private LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> getPaths(GStringNode originNode, GStringNode startingNode, HashSet<GStringNode> nodesToRemove) {
        LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> result = new LinkedList<>();
        LinkedList<GStringEdge> currentPath = new LinkedList<>();
        HashSet<GStringNode> visitedNodes = new HashSet<>();
        getPaths(originNode, startingNode, result, currentPath, visitedNodes, new NodeMatchCount(), nodesToRemove);
        return result;
    }

    private void getPaths(
            GStringNode originNode,
            GStringNode startingNode,
            LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> result,
            LinkedList<GStringEdge> currentPath,
            HashSet<GStringNode> visitedNodes,
            NodeMatchCount counts,
            HashSet<GStringNode> nodesToRemove
    ) {
        visitedNodes.add(startingNode);
        nodesToRemove.add(startingNode);

        NodeMatchCount newCount = new NodeMatchCount(
                counts.specialAtomCount + startingNode.specialAtomCount,
                counts.specialBondCount,
                counts.branchCount
        );

        //end of path
        if (startingNode.edges.size() == 1) {
            result.add(new Pair<>(((LinkedList<GStringEdge>)currentPath.clone()), newCount));
        }

        HashSet<GStringEdge> branchEdges = new HashSet<>();

        //handle branches
        if (startingNode.edges.size() > 2) {
            for (GStringEdge edge : startingNode.edges) {
                GStringNode otherNode = edge.getOther(startingNode);
                if (otherNode.equals(originNode)) {
                    continue;
                }

                //Branch!
                if (otherNode.edges.size() == 1) {
                    //if there was no continuation, mark last branch as the end of path
                    //-2: 1 for origin edge, 1 for current edge. It means that all previous edges were branches
                    if (edge.equals(startingNode.edges.getLast()) && branchEdges.size() == startingNode.edges.size() - 2) {
                        break;
                    }
                    branchEdges.add(edge);
                    newCount.branchCount++;
                    newCount.specialAtomCount += otherNode.specialAtomCount;
                    if (edge.isSpecial) {
                        newCount.specialBondCount++;
                    }
                }
            }
        }


        for (GStringEdge edge : startingNode.edges) {
            GStringNode otherNode = edge.getOther(startingNode);
            if (otherNode.equals(originNode)) {
                continue;
            }

            if (branchEdges.contains(edge)) {
                continue;
            }


            NodeMatchCount edgeNewCount = (NodeMatchCount)newCount.clone();
            if (edge.isSpecial) {
                edgeNewCount.specialBondCount++;
            }

            currentPath.addLast(edge);

            //We met either large cycle or non-temp node -> report it as new path!
            if (visitedNodes.contains(otherNode) || !otherNode.type.equals(GStringNodeType.TEMP)) {
                result.add(new Pair<>(((LinkedList<GStringEdge>)currentPath.clone()), edgeNewCount));
                currentPath.removeLast();
                continue;
            }

            //Edge leads to TEMP node -> continue
            getPaths(startingNode, otherNode, result, currentPath, visitedNodes, edgeNewCount, nodesToRemove);
            currentPath.removeLast();
        }

        visitedNodes.remove(startingNode);
    }

    //Duplicates the node and each new node will have only 1 TEMP neighbour. Returns new node and edge
    //originNode should be PATH node with exactly 2 TEMP neighbours and 1 non-TEMP neighbour
    private NewNodeEdgeResult splitNode(GStringNode originNode, GStringNode originNodeContinuation, GStringNode originSpecialNode) {
        assert originNode.type.equals(GStringNodeType.PATH);
        assert originNode.size == 1;
        assert originNodeContinuation.type.equals(GStringNodeType.TEMP);
        assert !originSpecialNode.type.equals(GStringNodeType.TEMP);

        GStringNode newNode = new GStringNode(
            GStringNodeType.PATH,
            1,
            originNode.specialAtomCount,
            originNode.specialBondCount,
            0
        );

        nodes.add(newNode);

        GStringEdge toBeRemovedEdge = null;
        GStringEdge duplicateEdge;
        GStringEdge originNewEdge = null;

        int continuationVisited = 0;
        int nonTempVisited = 0;
        int otherTempVisited = 0;

        for (GStringEdge edge : originNode.edges) {
            GStringNode otherNode = edge.getOther(originNode);
            if (otherNode.equals(originNodeContinuation)) {
                //nothing, this edge should be unchanged

                continuationVisited++;
            }
            else if (!otherNode.type.equals(GStringNodeType.TEMP)) {
                //we need to duplicate this edge to new node
                duplicateEdge = new GStringEdge(edge.isSpecial, otherNode, newNode);
                newNode.edges.add(duplicateEdge);

                if (otherNode.equals(originSpecialNode)) {
                    originNewEdge = duplicateEdge;
                }
                else {
                    otherNode.edges.add(duplicateEdge);
                }

                nonTempVisited++;
            }
            else {
                edge.reconnectEdge(originNode, newNode);
                newNode.edges.add(edge);
                toBeRemovedEdge = edge;

                otherTempVisited++;
            }
        }

        assert continuationVisited == 1;
        assert nonTempVisited == originNode.edges.size() - 2;
        assert otherTempVisited == 1;
        assert originNewEdge != null;

        originNode.edges.remove(toBeRemovedEdge);

        return new NewNodeEdgeResult(newNode, originNewEdge);
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
