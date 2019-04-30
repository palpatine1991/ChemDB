import GString.TreeNode;
import Utils.GraphSerializer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import shared.QueryUtils;
import shared.SDFParser;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws CDKException {
        SDFParser parser = new SDFParser("../chembl_24.sdf");
//        SDFParser parser = new SDFParser("../chembl_test1.sdf");
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
//            db.put("xxx", sp.parseSmiles("C[CH](C=CC[CH](O)C12CC3CC(CC(C3)C1)C2)[CH]4CC[CH]5C(CCC[C]45C)=CC=C6C[CH](O)C(=C)[CH](O)C6"));
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
        //String query = "CCCC";

        String[] querySet = QueryUtils.query16;
        //endregion

        if (false) {
            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            HashMap<String, IAtomContainer> querydb = new HashMap<>();
            querydb.put("0", sp.parseSmiles(QueryUtils.query4_1));
            querydb.put("1", sp.parseSmiles(QueryUtils.query4_2));
            querydb.put("2", sp.parseSmiles(QueryUtils.query4_3));
            querydb.put("3", sp.parseSmiles(QueryUtils.query4_4));
            querydb.put("4", sp.parseSmiles(QueryUtils.query4_5));
            querydb.put("5", sp.parseSmiles(QueryUtils.query4_6));
            querydb.put("6", sp.parseSmiles(QueryUtils.query4_7));
            querydb.put("7", sp.parseSmiles(QueryUtils.query4_8));
            querydb.put("8", sp.parseSmiles(QueryUtils.query4_9));
            querydb.put("9", sp.parseSmiles(QueryUtils.query4_10));
            GraphSerializer.serializeDB("testDB", db);
            GraphSerializer.serializeDB("queryDB", querydb);
            return;
        }

        IDBTester tester = new GraphGrepSXDBTester();
//        IDBTester tester = new GStringDBTester();
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
        //System.out.println(TreeNode.count);
        //((GStringDBTester) tester).gstring.root = null;
        System.gc();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory() - usedMemoryBefore;
        System.out.print("Build Index Consumed memory: ");
        System.out.println(usedMemory / 1048576);
        //endregion

        //region getting candidate set


        int counter = 1;
        for (String query : querySet) {
            System.out.println("-----------------" + counter++ + "-------------------");

            try {
                SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                queryContainer = sp.parseSmiles(query);
            } catch (InvalidSmilesException e) {
                System.err.println(e.getMessage());
            }

            startTime = System.nanoTime();

            HashMap<String, IAtomContainer> candidateSet = tester.getCandidateSet(queryContainer);

            estimatedTime = System.nanoTime() - startTime;
            System.out.print("Obtaining candidate set Time: ");
            System.out.println(estimatedTime / 1000000);
            System.out.print("Candidate set size: ");
            if (candidateSet != null) {
                System.out.println(candidateSet.size());
            }
            else {
                System.out.println();
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
                //System.out.println(s);
            }
        }

    }
}
