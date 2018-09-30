package GString;

import javafx.util.Pair;
import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.Intractable;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;

import java.util.*;

public class GStringGraph {
    private static CycleFinder cycleFinder = Cycles.mcb();

    public LinkedList<GStringNode> nodes = new LinkedList<>();

    private HashMap<IAtom, LinkedList<GStringNode>> processedAtoms = new HashMap<>();
    private HashMap<IAtomContainer, GStringNode> processedRings = new HashMap<>();
    private IAtomContainer molecule;
    private IRingSet completeRingSet;
    private int starFanoutTreshold = 4;

    private HashSet<GStringNode> nodesToRemove = new HashSet<>();
    private LinkedList<GStringPathResults> pathResults = new LinkedList<>();

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
        findAndMarkPathEnds();
        findAndProcessPaths();
        processBranches();
        removeTempCycles();
    }

    private void removeTempCycles() {
        ListIterator<GStringNode> iterator = nodes.listIterator();

        while (iterator.hasNext()) {
            GStringNode node = iterator.next();

            if (node.type.equals(GStringNodeType.CYCLE) && node.size == 0) {
                iterator.remove();
                for (GStringEdge edge : node.edges) {
                    GStringNode otherNode = edge.getOther(node);
                    otherNode.edges.remove(edge);
                }
            }
        }
    }

    private void findAndMarkPathEnds() {
        ListIterator<GStringNode> iterator = nodes.listIterator();

        while (iterator.hasNext()) {
            GStringNode node = iterator.next();
            boolean pathEnd = false;

            if (!node.type.equals(GStringNodeType.TEMP)) {
                continue;
            }

            //lone atom
            if (node.edges.size() == 0) {
                node.type = GStringNodeType.PATH;
                node.size = 1;
            }
            else if (node.edges.size() > 1) {
                continue;
            }
            //possible end of path
            else {
                GStringNode otherNode = node.edges.getFirst().getOther(node);

                if (!otherNode.type.equals(GStringNodeType.TEMP)) {
                    //nothing - definitely a branch
                }
                //if neighbour has only 1 other neighbour -> definitely a path end
                else if (otherNode.edges.size() <= 2) {
                    pathEnd = true;
                }
                //we need to distinguish whether it is a branch or path end
                else {
                    //We need at least 2 other neighours which are not leafs to proof that it is a branch
                    int branchFault = 0;
                    for (GStringEdge edge : otherNode.edges) {
                        GStringNode otherOtherNode = edge.getOther(otherNode);

                        if (node.equals(otherOtherNode)) {
                            continue;
                        }

                        if (otherOtherNode.edges.size() > 1) {
                            branchFault++;
                        }
                    }

                    if (branchFault < 2) {
                        pathEnd = true;
                    }
                }
            }

            if (pathEnd) {
                GStringNode temp = new GStringNode(GStringNodeType.CYCLE, 0, 0, 0, 0);
                GStringEdge tempEdge = new GStringEdge(false, temp, node);
                tempEdge.registerEdgeToNodes();
                iterator.add(temp);
            }
        }
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

        //if there are no special nodes -> add 1 on random place
        if (processedNodes.size() == 0) {
            GStringNode firstNode = nodes.getFirst();

            GStringNode temp = new GStringNode(GStringNodeType.CYCLE, 0, 0, 0, 0);
            GStringEdge tempEdge = new GStringEdge(false, temp, firstNode);
            tempEdge.registerEdgeToNodes();
            nodes.add(temp);
            processedNodes.add(temp);
        }

        for (GStringNode processedNode : processedNodes) {
            processAdjacentPaths(processedNode);
        }

        createPathNodes();
        nodes.removeAll(nodesToRemove);
    }

    private void createPathNodes() {
        //All paths which connect special nodes (stars, cycles) are duplicated
        //Therefore we need to check those duplicities
        HashMap<GStringNode, LinkedList<GStringDuplicityCheck>> duplicities = new HashMap<>();

        for (GStringPathResults results : pathResults) {
            for (Pair<LinkedList<GStringEdge>, NodeMatchCount> result : results.pathResults) {
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

                //If both end nodes are special type, we can expect duplicity
                if (endNode != null && duplicities.containsKey(results.originNode)) {
                    ListIterator<GStringDuplicityCheck> iterator = duplicities.get(results.originNode).listIterator();
                    boolean skipResult = false;

                    while (iterator.hasNext()) {
                        GStringDuplicityCheck duplicity = iterator.next();

                        if (!duplicity.otherNode.equals(endNode)) {
                            continue;
                        }
                        if (duplicity.size != pathSize) {
                            continue;
                        }
                        if (duplicity.counts.specialAtomCount != count.specialAtomCount) {
                            continue;
                        }
                        if (duplicity.counts.specialBondCount != count.specialBondCount) {
                            continue;
                        }
                        if (duplicity.counts.branchCount != count.branchCount) {
                            continue;
                        }

                        //Remove duplicity note
                        iterator.remove();
                        skipResult = true;
                    }

                    //skip current path processing
                    if (skipResult) {
                        continue;
                    }
                }

                if (endNode != null) {
                    if (!duplicities.containsKey(endNode)) {
                        duplicities.put(endNode, new LinkedList<>());
                    }

                    duplicities.get(endNode).add(new GStringDuplicityCheck(results.originNode, (NodeMatchCount) count.clone(), pathSize));
                }

                GStringNode pathNode = new GStringNode(GStringNodeType.PATH, pathSize, count.specialAtomCount, count.specialBondCount, count.branchCount);
                nodes.add(pathNode);

                GStringEdge newEdge1 = new GStringEdge(results.startingEdge.isSpecial, pathNode, results.originNode);
                results.originNode.edges.remove(results.startingEdge);
                newEdge1.registerEdgeToNodes();

                if (endNode != null) {
                    endNode.edges.remove(lastEdge);
                    GStringEdge newEdge2 = new GStringEdge(lastEdge.isSpecial, pathNode, endNode);
                    newEdge2.registerEdgeToNodes();
                }
            }
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

            pathResults.add(new GStringPathResults(getPaths(node, otherNode), node, startingEdge));
        }
    }

    private LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> getPaths(GStringNode originNode, GStringNode startingNode) {
        LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> result = new LinkedList<>();
        LinkedList<GStringEdge> currentPath = new LinkedList<>();
        HashSet<GStringNode> visitedNodes = new HashSet<>();
        getPaths(originNode, startingNode, result, currentPath, visitedNodes, new NodeMatchCount());
        return result;
    }

    private void getPaths(
            GStringNode originNode,
            GStringNode startingNode,
            LinkedList<Pair<LinkedList<GStringEdge>, NodeMatchCount>> result,
            LinkedList<GStringEdge> currentPath,
            HashSet<GStringNode> visitedNodes,
            NodeMatchCount counts
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
            getPaths(startingNode, otherNode, result, currentPath, visitedNodes, edgeNewCount);
            currentPath.removeLast();
        }

        visitedNodes.remove(startingNode);
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
