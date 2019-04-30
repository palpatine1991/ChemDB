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

    public static String query4_1 = "c:c:c:c";
    public static String query4_2 = "C=NCC";
    public static String query4_3 = "S(=O)(=O)C";
    public static String query4_4 = "C=CC=C";
    public static String query4_5 = "N=CCC";
    public static String query4_6 = "CN=CO";
    public static String query4_7 = "SCCC";
    public static String query4_8 = "c:n:c:c";
    public static String query4_9 = "CSCC";
    public static String query4_10 = "CSC=C";

    public static String query8_1 = "COc1ccccc1";
    public static String query8_2 = "CCCCCCCC";
    public static String query8_3 = "c:c:c:c:c:c:c:c";
    public static String query8_4 = "S(CCC)(CCC)(C)";
    public static String query8_5 = "C(=O)NCCCCC";
    public static String query8_6 = "c1cc(C(=O))ccc1";
    public static String query8_7 = "CCC(=O)C(=O)NC";
    public static String query8_8 = "c2c(Cl)cccc2Cl";
    public static String query8_9 = "C(C)(C)C(C)(C)C(=O)";
    public static String query8_10 = "S(CNCC)(C=O)(C)";

    public static String query16_1 = "C(=O)c1cc(CCCCC)ccc1";
//    public static String query16_1 = "CNC(=O)c1cc(C(=O)CCCC)ccc1";
    public static String query16_2 = "CCCCCCCCCCCCCCCC";
    public static String query16_3 = "c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c";
    public static String query16_4 = "c1cccc2c1ccc3c2cccc3CC";
    public static String query16_5 = "S(CC)(=O)(=O)Nc1cc(CCCC)ccc1";
    public static String query16_6 = "C(C)(C)C(C)(C)C(C)(C)C(C)(CCCCC)";
    public static String query16_7 = "C(O)CCCC(=C)CCCc1ccccc1";
    public static String query16_8 = "CCCCCNCCCCCCCNCC";
    public static String query16_9 = "c1ccccc1CCc2cc(CC)ccc2";
    public static String query16_10 = "Oc1cccc2Cc3ccccc3C(=O)c12";

    public static String query24_1 = "C(C)NC(=O)C(NC(=O)OC)CCCCNC(=O)c1ccccc1";
    public static String query24_2 = "CCCCCCCCCCCCCCCCCCCCCCCC";
    public static String query24_3 = "c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c:c";
    public static String query24_4 = "c1cccc2c1ccc3c2ccc4c3ccc5c4cccc5CC";
    public static String query24_5 = "S(C)(=O)(=O)Nc1cc(C(c2c(=O)oc(CC)cc2)CCC)ccc1";
    public static String query24_6 = "C(C)(C)C(C)(C)CCCC(C(NCCCCC)=O)(CCCCCC)";
    public static String query24_7 = "C(O)C(O)C(O)C(O)CCCC(=C)C(O)C(C)Cc1ccccc1";
    public static String query24_8 = "CCCCCNCCCCCCCNCCCCCCCCCC";
    public static String query24_9 = "c1ccccc1CCc2cc(CCCNc3ccccc3)ccc2";
    public static String query24_10 = "Oc1cccc2Cc3ccc(Cc4ccccc4)c(O)c3C(=O)c12";

    public static String[] query4 = {query4_1, query4_2, query4_3, query4_4, query4_5, query4_6, query4_7, query4_8, query4_9, query4_10};
    public static String[] query8 = {query8_1, query8_2, query8_3, query8_4, query8_5, query8_6, query8_7, query8_8, query8_9, query8_10};
    public static String[] query16 = {query16_1, query16_2, query16_3, query16_4, query16_5, query16_6, query16_7, query16_8, query16_9, query16_10};
    public static String[] query24 = {query24_1, query24_2, query24_3, query24_4, query24_5, query24_6, query24_7, query24_8, query24_9, query24_10};

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
