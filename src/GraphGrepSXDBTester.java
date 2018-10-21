import GraphGrepSX.GraphGrepSX;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;

public class GraphGrepSXDBTester implements IDBTester {
    GraphGrepSX gsx;

    public void buildIndex(HashMap<String, IAtomContainer> db) {
        gsx = new GraphGrepSX(db, 6);
        gsx.buildIndex();
//        gsx.test(10);
    }

    public HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query) {
        return gsx.getCandidateSet(query);
    }

    public ArrayList<String> getResults(IAtomContainer query, HashMap<String, IAtomContainer> candidateSet, String queryString) {
        return QueryUtils.getMatchedIds(queryString, candidateSet);
    }
}
