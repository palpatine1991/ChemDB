package shared;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QueryUtils {

    public static String query4_1 = "cccc";
    public static String query4_2 = "C=NCC";
    public static String query4_3 = "S(C)(C)(C)";
    public static String query4_4 = "C=CC=C";
    public static String query4_5 = "N=CCC";
    public static String query4_6 = "CN=CO";
    public static String query4_7 = "cccc"; //TODO change!
    public static String query4_8 = "cncc";
    public static String query4_9 = "CSCC";
    public static String query4_10 = "CSC=C";

    public static String query24_1 = "c1cccc2ccnc(c3cc(C(=O)NCCCCC)ccc3)c12";
    public static String query24_2 = "CCCCCCCCCCCCCCCCCCCCCCCC";

    public static ArrayList<String> getMatchedIds(String query, HashMap<String, IAtomContainer> candidateSet) {
        SMARTSQueryTool queryTool;
        queryTool = new SMARTSQueryTool(query, DefaultChemObjectBuilder.getInstance());
        ArrayList<String> result = new ArrayList<>();

        Aromaticity aromaticity = Constants.getAromaticityModel();

        queryTool.setAromaticity(aromaticity);

        try {
            for (Map.Entry<String, IAtomContainer> entry : candidateSet.entrySet()) {
                if (queryTool.matches(entry.getValue())) {
                    result.add(entry.getValue().getProperty("chembl_id"));
                }
            }
        } catch(Exception e) {
            System.out.println(e);
        }

        return result;
    }
}
