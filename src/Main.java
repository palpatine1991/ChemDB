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


        IAtomContainer queryContainer = null;

        String[] querySet = QueryUtils.query16;

        //turn on to execute graph serialization for GIRAS
        if (false) {
            SmilesParser sp  = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            HashMap<String, IAtomContainer> querydb4 = new HashMap<>();
            HashMap<String, IAtomContainer> querydb8 = new HashMap<>();
            HashMap<String, IAtomContainer> querydb16 = new HashMap<>();
            HashMap<String, IAtomContainer> querydb24 = new HashMap<>();

            int order = 1;

            for(String query : QueryUtils.query4) {
                querydb4.put(Integer.toString(order++), sp.parseSmiles(query));
            }

            order = 1;

            for(String query : QueryUtils.query8) {
                querydb8.put(Integer.toString(order++), sp.parseSmiles(query));
            }

            order = 1;

            for(String query : QueryUtils.query16) {
                querydb16.put(Integer.toString(order++), sp.parseSmiles(query));
            }

            order = 1;

            for(String query : QueryUtils.query24) {
                querydb24.put(Integer.toString(order++), sp.parseSmiles(query));
            }
            GraphSerializer.serializeDB("testDB", db);
            GraphSerializer.serializeDB("queryDB4", querydb4);
            GraphSerializer.serializeDB("queryDB8", querydb8);
            GraphSerializer.serializeDB("queryDB16", querydb16);
            GraphSerializer.serializeDB("queryDB24", querydb24);
            return;
        }

//        IDBTester tester = new GraphGrepSXDBTester();
//        IDBTester tester = new GStringDBTester();
        IDBTester tester = new SqlDBTester();

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



        int counter = 1;
        for (String query : querySet) {
            //region getting candidate set

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
