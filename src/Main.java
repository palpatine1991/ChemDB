import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

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

//        try {
//            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
//            db = new HashMap<>();
//            IAtomContainer x = sp.parseSmiles("c1ccccc1");
//            db.put("xxx", sp.parseSmiles("Oc1ccc2c3ccc4cccc5ccc(cc2c1O)c3c45"));
//        } catch (InvalidSmilesException e) {
//            System.err.println(e.getMessage());
//        }

        //region SMILES testing
        IAtomContainer queryContainer = null;
        //String query = "c1cc(-O-C(Cc1ccccc1)-C)ccc1"; //no match, but GraphGrepSX has a lot a false positives, GString has empty set!
        //String query = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC";
        //String query = "c1ccccc1c2ccccc2c3ccccc3c4ccccc4c5ccccc5";
        //String query = "n1ccccc1c2ccccc2";
        //String query = "Oc1ccc(\\C=C(/C#N)\\C(=O)OC\\C=C\\c2ccccc2)cc1O"; //1 exact match
        //String query = "SCCCCC(=O)O";
        //String query = "N1-C-N=C-C=C1"; //GString has higher candidate set because of only 1 cycle which is almost everywhere
        String query = "c1ccc2c3ccc4cccc5ccc(cc2c1)c3c45";

//        String query = QueryUtils.query24_2;


        try {
            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            queryContainer = sp.parseSmiles(query);
        } catch (InvalidSmilesException e) {
            System.err.println(e.getMessage());
        }
        //endregion

//        IDBTester tester = new GraphGrepSXDBTester();
        IDBTester tester = new GStringDBTester();
//        IDBTester tester = new SqlDBTester();

        //region index building
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.nanoTime();

        tester.buildIndex(db);

        long estimatedTime = System.nanoTime() - startTime;
        System.out.print("Build Index Time: ");
        System.out.println(estimatedTime / 1000000);
        System.gc();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory() - usedMemoryBefore;
        System.out.print("Build Index Consumed memory: ");
        System.out.println(usedMemory / 1048576);
        //endregion

        //region getting candidate set
        startTime = System.nanoTime();

        HashMap<String, IAtomContainer> candidateSet = tester.getCandidateSet(queryContainer);

        estimatedTime = System.nanoTime() - startTime;
        System.out.print("Obtaining candidate set Time: ");
        System.out.println(estimatedTime / 1000000);
        System.out.print("Candidate set size: ");
        if (candidateSet != null) {
            System.out.println(candidateSet.size());
        }

        //endregion

        //region getting final results
        startTime = System.nanoTime();

        ArrayList<String> results = tester.getResults(queryContainer, candidateSet, query);

        estimatedTime = System.nanoTime() - startTime;
        System.out.print("Verification Time: ");
        System.out.println(estimatedTime / 1000000);
        System.out.print("Result set size: ");
        System.out.println(results.size());
        //endregion

        for (String s  : results) {
            System.out.println(s);
        }
    }
}
