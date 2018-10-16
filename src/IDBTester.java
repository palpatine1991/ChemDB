import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;

public interface IDBTester {
    void buildIndex(HashMap<String, IAtomContainer> db);
    HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query);
    ArrayList<String> getResults(IAtomContainer query, HashMap<String, IAtomContainer> candidateSet, String queryString);
}
