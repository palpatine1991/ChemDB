package GString;

import org.openscience.cdk.Bond;
import org.openscience.cdk.exception.Intractable;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class GStringGraph {
    private static CycleFinder cycleFinder = Cycles.mcb();

    public LinkedList<GStringNode> nodes = new LinkedList<>();

    private HashMap<IAtom, LinkedList<GStringNode>> processedAtoms = new HashMap<>();
    private HashMap<IAtomContainer, GStringNode> processedRings = new HashMap<>();
    private HashSet<IAtomContainer> openRings = new HashSet<>();
    private IAtomContainer molecule;
    private IRingSet completeRingSet;

    //TODO: build graph
    public GStringGraph(IAtomContainer molecule) {
        this.molecule = molecule;

        try {
            Cycles cycles = cycleFinder.find(molecule, 10);
            completeRingSet = cycles.toRingSet();

            completeRingSet.atomContainers().forEach((ring) -> {
                processRing(ring);
            });

            molecule.atoms().forEach((atom) -> {
                processAtom(atom);
            });

            //TODO steps after parsing rings
        }
        catch (Intractable e) {
            // ignore error - MCB should never be intractable
        }
    }

    private void processAtom(IAtom atom) {
        LinkedList<GStringNode> linkedNodes;

        if (!processedAtoms.containsKey(atom)) {
            int nonDefaultCount = (isDefaultAtom(atom)) ? 0 : 1;

            GStringNode node = new GStringNode(GStringNodeType.TEMP, 1, nonDefaultCount, 0, 0);
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
        int specialAtomCount = 0;
        int specialBondCount = 0;

        //setting special bond count
        for (Iterator<IBond> i = ring.bonds().iterator(); i.hasNext();) {
            IBond bond = i.next();

            bond.setIsInRing(true);
            if (!isDefaultBond(bond)) {
                specialBondCount++;
            }
        }
        node.specialBondCount = specialBondCount;

        //setting special atom count and finding neighbours
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

            IRingSet atomRings = completeRingSet.getRings(atom);
            atomRings.atomContainers().forEach((ringContainer) -> {
                if (ringContainer == ring) {
                    return;
                }

                if (processedRings.containsKey(ringContainer) && !node.isNeighbour(processedRings.get(ringContainer))) {
                    GStringEdge edge = new GStringEdge(false, node, processedRings.get(ringContainer));
                    edge.registerEdgeToNodes();
                }
            });
        }
        node.specialAtomCount = specialAtomCount;

    }

    private boolean isDefaultAtom(IAtom atom) {
        return atom.getSymbol().equals("C");
    }

    private boolean isDefaultBond(IBond bond) {
        return bond.getOrder().equals(Bond.Order.SINGLE);
    }
}
