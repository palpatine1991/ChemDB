import GString.GStringGraph;
import org.openscience.cdk.Bond;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.Intractable;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectedComponents;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws CDKException {
        SDFParser parser = new SDFParser("../chembl_test1.sdf");
        HashMap<String, IAtomContainer> db = null;

        try {
            db = parser.getDB("chembl_id");
        } catch (FileNotFoundException exc) {
            System.out.println("DB file not found");
        }


        //region Playing with SSSR
        AllRingsFinder arf = new AllRingsFinder();
        int maxRings = 0;
        String maxRingsId = "";
        IRingSet maxRingsSet = null;

        int count = 0;

        CycleFinder cf = Cycles.mcb();

        for (IAtomContainer m : db.values()) {
//            if (!m.getProperty("chembl_id").equals("CHEMBL39800")) {
//                continue;
//            }

            String id = m.getProperty("chembl_id");
            System.out.println(id);

            //testing connectivity
            int[][] adj = GraphUtil.toAdjList(m);
            ConnectedComponents cc = new ConnectedComponents(adj);
            int[] components = cc.components();

            boolean nonconnected = false;

            for (int v = 0; v < adj.length; v++) {
                if (components[v] > 1) {
                    nonconnected = true;
                    break;
                }
            }

            if (nonconnected) {
                //TODO: handle non-connected structures
                continue;
            }

            GStringGraph g = new GStringGraph(m);
            System.out.println();
        }

        for (IAtomContainer m : db.values()) {
            try {
                Cycles cycles = cf.find(m, 10);
                IRingSet rs  = cycles.toRingSet();
                if (rs.getAtomContainerCount() > maxRings && rs.getAtomContainerCount() < 10) {
                    maxRings = rs.getAtomContainerCount();
                    maxRingsId = m.getProperty("chembl_id");
                    maxRingsSet = rs;

                    System.out.println(maxRingsId);
                    GStringGraph g = new GStringGraph(m);
                    System.out.println();
                }


            } catch (Intractable e) {
                // ignore error - MCB should never be intractable
            }
        }


        System.out.println(maxRings);
        System.out.println(maxRingsId);
        maxRingsSet.atomContainers().forEach((a) -> {
            System.out.println(a.getAtomCount());
        });
        //endregion

        //region Simple cycle test
//        IAtomContainer molecule = new AtomContainer();
//        Atom atom1 = new Atom();
//        atom1.setSymbol("C");
//        Atom atom2 = new Atom();
//        atom2.setSymbol("O");
//        Atom atom3 = new Atom();
//        atom3.setSymbol("N");
//        Bond bond1 = new Bond();
//        bond1.setOrder(IBond.Order.DOUBLE);
//        bond1.setAtoms(new IAtom[]{atom1, atom2});
//        Bond bond2 = new Bond();
//        bond2.setOrder(IBond.Order.TRIPLE);
//        bond2.setAtoms(new IAtom[]{atom2, atom3});
//        Bond bond3 = new Bond();
//        bond3.setOrder(IBond.Order.SINGLE);
//        bond3.setAtoms(new IAtom[]{atom3, atom1});
//
//
//        molecule.addAtom(atom1);
//        molecule.addAtom(atom2);
//        molecule.addAtom(atom3);
//        molecule.addBond(bond1);
//        molecule.addBond(bond2);
//        molecule.addBond(bond3);
//
//        db = new HashMap<>();
//        db.put("xxx", molecule);
        //endregion

        IAtomContainer query = null;

        try {
            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            query = sp.parseSmiles("C=NOCCOCC");
        } catch (InvalidSmilesException e) {
            System.err.println(e.getMessage());
        }

        //region EXACT MATCHING
        /*UniversalIsomorphismTester tester = new UniversalIsomorphismTester();
        int matchCount = 0;

        for (Map.Entry<String, IAtomContainer> entry : db.entrySet()) {
            if (tester.isSubgraph(entry.getValue(), query)) {
                matchCount++;
                //System.out.println(entry.getValue().getProperty("chembl_id").toString());
            }
        }

        System.out.println(matchCount);*/
        //endregion

        //region GraphGrepSX.GraphGrepSX
        /*GraphGrepSX.GraphGrepSX gsx = new GraphGrepSX.GraphGrepSX(db, 6);
        gsx.buildIndex();
        HashMap<String, IAtomContainer> candidateSet = gsx.getCandidateSet(query);

        for (Map.Entry<String, IAtomContainer> entry : candidateSet.entrySet()) {
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
//            System.out.println("------------------");
        }

        System.out.println(candidateSet.size());*/
        //gsx.test(9);
        //endregion
    }
}
