import GString.GString;
import GString.GStringGraph;
import org.openscience.cdk.AtomContainer;
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
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws CDKException {
        //SDFParser parser = new SDFParser("../chembl_24.sdf");
        SDFParser parser = new SDFParser("../chembl_test1.sdf");
        HashMap<String, IAtomContainer> db = null;

        try {
            db = parser.getDB("chembl_id");
        } catch (FileNotFoundException exc) {
            System.out.println("DB file not found");
        }


//        System.out.println(maxRings);
//        System.out.println(maxRingsId);
//        maxRingsSet.atomContainers().forEach((a) -> {
//            System.out.println(a.getAtomCount());
//        });
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

        IAtomContainer queryContainer = null;
        //String query = "c1cc(-O-C(Cc1ccccc1)-C)ccc1"; //no match, but GraphGrepSX has a lot a false positives, GString has empty set!
        //String query = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        String query = "c1ccccc1c2ccccc2c3ccccc3c4ccccc4c5ccccc5";
        //String query = "Oc1ccc(\\C=C(/C#N)\\C(=O)OC\\C=C\\c2ccccc2)cc1O"; //1 exact match
        //String query = "SCCCCC(=O)O";
        //String query = "N1-C-N=C-C=C1"; //GString has higher candidate set because of only 1 cycle which is almost everywhere
        SMARTSQueryTool queryTool;
        queryTool = new SMARTSQueryTool(query, DefaultChemObjectBuilder.getInstance());

        try {
            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            queryContainer = sp.parseSmiles(query);
        } catch (InvalidSmilesException e) {
            System.err.println(e.getMessage());
        }

        //region EXACT MATCHING
        int matchCount = 0;

        for (Map.Entry<String, IAtomContainer> entry : db.entrySet()) {
            if (queryTool.matches(entry.getValue())) {
                matchCount++;
                System.out.println(entry.getValue().getProperty("chembl_id").toString());
            }
        }

        System.out.print("Exact match count: ");
        System.out.println(matchCount);
        //endregion

        //region GraphGrepSX.GraphGrepSX
        GraphGrepSX.GraphGrepSX gsx = new GraphGrepSX.GraphGrepSX(db, 6);
        gsx.buildIndex();
        HashMap<String, IAtomContainer> candidateSet = gsx.getCandidateSet(queryContainer);

        for (Map.Entry<String, IAtomContainer> entry : candidateSet.entrySet()) {
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
//            System.out.println("------------------");
        }

        System.out.print("GarphGrepSX candidate set size: ");
        System.out.println(candidateSet.size());
        for(String id : candidateSet.keySet()) {
            System.out.println(id);
        }
        //gsx.test(9);
        //endregion

        //region GString
        GString gstring = new GString(db, 5);
        gstring.buildIndex();
        HashMap<String, IAtomContainer> gStringCandidateSet = gstring.getCandidateSet(queryContainer);
        System.out.print("GString candidate set size: ");
        System.out.println(gStringCandidateSet.size());
        for(String id : gStringCandidateSet.keySet()) {
            System.out.println(id);
        }
        //gstring.test(1);
        //endregion
    }
}
