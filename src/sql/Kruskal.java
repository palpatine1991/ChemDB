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
    IAtomContainer molecule;

    HashMap<IBond, BondPrice> bondToPricaMapping = new HashMap<>();

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

    public boolean isBondInSpanningTree(IBond bond) {
        BondPrice bondPrice = bondToPricaMapping.get(bond);
        return bondPrice.presentInSpanningTree;
    }

    public IBond getLowestPriceBond() {
        return bondList.get(0).bond;
    }

    public ArrayList<IBond> getOrderBondList(Iterable<IBond> bonds) {
        ArrayList<BondPrice> bondPrices = new ArrayList<>();

        for (IBond bond : bonds) {
            bondPrices.add(bondToPricaMapping.get(bond));
        }

        Collections.sort(bondPrices);

        ArrayList<IBond> result = new ArrayList<>();

        for (BondPrice bond : bondPrices) {
            result.add(bond.bond);
        }

        return result;
    }

    private void makeAndSortBondList() {
        int bondIndex = 0;
        for (IBond bond : molecule.bonds()) {
            bond.setID(Integer.toString(bondIndex++));
            char bondCharacter = SqlHandler.getBondCharacter(bond);
            String atom1Symbol = bond.getAtom(0).getSymbol();
            String atom2Symbol = bond.getAtom(1).getSymbol();

            int price = statistics.getOrDefault(atom1Symbol + bondCharacter + atom2Symbol, 0);
            BondPrice bondPrice = new BondPrice(bond, price);
            bondList.add(bondPrice);
            bondToPricaMapping.put(bond, bondPrice);
        }

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
