package sql;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Kruskal {
    private DisjointSet disjointSet;
    List<BondPrice> bondList = new ArrayList<>();
    BondPrice[] orderedBonds;
    IAtomContainer molecule;

    HashMap<String, Integer> statistics;

    public Kruskal(IAtomContainer molecule, HashMap<String, Integer> statistics) {
        int atomId = 0;
        for (IAtom atom : molecule.atoms()) {
            atom.setID(Integer.toString(atomId++));
        }

        disjointSet = new DisjointSet(atomId);
        this.statistics = statistics;
        this.molecule = molecule;

        makeAndSortBondList();
        makeSpanningTree();
    }

    private void makeAndSortBondList() {
        int bondIndex = 0;
        for (IBond bond : molecule.bonds()) {
            bond.setID(Integer.toString(bondIndex++));
            char bondCharacter = SqlHandler.getBondCharacter(bond);
            String atom1Symbol = bond.getAtom(0).getSymbol();
            String atom2Symbol = bond.getAtom(1).getSymbol();

            int price = statistics.get(atom1Symbol + bondCharacter + atom2Symbol);
            bondList.add(new BondPrice(bond, price));
        }

        orderedBonds = bondList.toArray(new BondPrice[0]);
        Collections.sort(bondList);
    }

    private void makeSpanningTree() {
        for (BondPrice bond : bondList) {
            int atom1Index = Integer.parseInt(bond.bond.getAtom(0).getID());
            int atom2Index = Integer.parseInt(bond.bond.getAtom(1).getID());
            if (disjointSet.find(atom1Index) != disjointSet.find(atom2Index)) {
                bond.presentInSpanningTree = true;
                disjointSet.union(atom1Index, atom2Index);
            }
        }
    }
}
