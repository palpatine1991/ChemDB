import GString.GString;
import org.openscience.cdk.interfaces.IAtomContainer;
import shared.QueryUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class GStringDBTester implements IDBTester {
    GString gstring;

    public void buildIndex(HashMap<String, IAtomContainer> db) {
        gstring = new GString(db, 5);
        gstring.buildIndex();
    }

    public HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query) {
        return gstring.getCandidateSet(query);
    }

    public ArrayList<String> getResults(IAtomContainer query, HashMap<String, IAtomContainer> candidateSet, String queryString) {
        return QueryUtils.getMatchedIds(queryString, candidateSet);
    }
}
