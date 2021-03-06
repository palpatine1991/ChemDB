import org.openscience.cdk.interfaces.IAtomContainer;
import sql.SqlHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SqlDBTester implements IDBTester {
    SqlHandler sql;

    @Override
    public void buildIndex(HashMap<String, IAtomContainer> db) {
        sql = new SqlHandler();
        int count = 0;
       // sql.clearDB();
        for (Map.Entry<String, IAtomContainer> entry : db.entrySet()) {
            sql.createMolecule(entry.getValue());
            count++;

            if (count % 1000 == 0) {
                System.out.println(count);
            }
        }

        sql.commitInsert();
    }

    @Override
    public HashMap<String, IAtomContainer> getCandidateSet(IAtomContainer query) {
        return null;
    }

    @Override
    public ArrayList<String> getResults(IAtomContainer query, HashMap<String, IAtomContainer> candidateSet, String queryString) {
        return sql.getQueryResults(query);
    }
}
