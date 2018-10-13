package sql;

import javafx.util.Pair;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class SqlHandler {
    final static String DB_URL= "jdbc:oracle:thin:@//localhost:1521/oracle1";
    final static String DB_USER = "C##admin";
    final static String DB_PASSWORD = "admin";

    int atomId = 0;

    Properties info;
    OracleDataSource ods;
    OracleConnection connection;
    StringBuilder insertQuery = new StringBuilder();
    int insertQueryLimit = 50;
    int insertQuerySize = 0;

    HashMap<String, Integer> statistics = new HashMap<>();

    public SqlHandler() {
        info = new Properties();
        info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
        info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
        info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");
        info.put(OracleConnection.CONNECTION_PROPERTY_THIN_NET_CHECKSUM_TYPES,
                "(MD5,SHA1,SHA256,SHA384,SHA512)");
        info.put(OracleConnection.CONNECTION_PROPERTY_THIN_NET_CHECKSUM_LEVEL,
                "REQUIRED");

        insertQuery.append("INSERT ALL ");

        try {
            OracleDataSource ods = new OracleDataSource();
            ods.setURL(DB_URL);
            ods.setConnectionProperties(info);
            connection = (OracleConnection) ods.getConnection();
            Statement statement = connection.createStatement();
            //statement.executeQuery("delete from bonds");
        } catch(Exception e) {
            System.out.println(e);
        }
    }
    public void commitInsert() {
        if (insertQuerySize == 0) {
            return;
        }

        insertQuery.append(" SELECT * FROM DUAL");

        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(insertQuery.toString());
            result.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        insertQuery = new StringBuilder();
        insertQuery.append("INSERT ALL ");

        insertQuerySize = 0;
    }

    public void createMolecule(IAtomContainer molecule) {
        for (IAtom atom : molecule.atoms()) {
            atom.setID(Integer.toString(atomId++));
        }

        for (IBond bond : molecule.bonds()) {
            char bondCharacter = getBondCharacter(bond);
            String atom1ID = bond.getAtom(0).getID();
            String atom2ID = bond.getAtom(1).getID();
            String atom1Symbol = bond.getAtom(0).getSymbol();
            String atom2Symbol = bond.getAtom(1).getSymbol();

            //Inserting bonds in default order
            insertQuery.append("INTO BONDS VALUES (");
            insertQuery.append(atom1ID);
            insertQuery.append(",");
            insertQuery.append(atom2ID);
            insertQuery.append(",'");
            insertQuery.append(atom1Symbol);
            insertQuery.append(bondCharacter);
            insertQuery.append(atom2Symbol);
            insertQuery.append("','");
            insertQuery.append((String)molecule.getProperty("chembl_id"));
            insertQuery.append("') INTO BONDS VALUES (");

            updateStatistics(atom1Symbol + bondCharacter + atom2Symbol);

            //Inserting bonds in reversed order
            insertQuery.append(atom2ID);
            insertQuery.append(",");
            insertQuery.append(atom1ID);
            insertQuery.append(",'");
            insertQuery.append(atom2Symbol);
            insertQuery.append(bondCharacter);
            insertQuery.append(atom1Symbol);
            insertQuery.append("','");
            insertQuery.append((String)molecule.getProperty("chembl_id"));
            insertQuery.append("')");

            if (!atom1Symbol.equals(atom2Symbol)) {
                updateStatistics(atom2Symbol + bondCharacter + atom1Symbol);
            }

            insertQuerySize += 2;
        }

        if (insertQuerySize > insertQueryLimit) {
            commitInsert();
        }
    }

    public void writeStatistics() {
        for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
            System.out.print(entry.getKey());
            System.out.print(": ");
            System.out.println(entry.getValue());
        }
    }

    public ArrayList<String> getQueryResults(IAtomContainer query) {
        ArrayList<String> result = new ArrayList<>();
        LinkedList<IBond> queue = new LinkedList<>();
        StringBuilder sqlQuery = new StringBuilder();

        //Bonds added to the queue
        HashSet<IBond> foundBonds = new HashSet<>();
        //Bonds added to SQL query
        HashSet<IBond> processedBonds = new HashSet<>();

        Kruskal kruskal = new Kruskal(query, statistics);

        sqlQuery.append("SELECT DISTINCT b0.compound_id FROM ");

        for (IBond bond : query.bonds()) {
            sqlQuery.append("BONDS ");
            sqlQuery.append(getBondTableId(bond));
            sqlQuery.append(", ");
        }

        //We need to remove last comma
        sqlQuery.setLength(sqlQuery.length() - 2);
        sqlQuery.append(" WHERE ");

        IBond minimalBond = kruskal.getLowestPriceBond();

        ArrayList<IBond> nonSpanningTreeBonds = new ArrayList<>();

        foundBonds.add(minimalBond);
        processedBonds.add(minimalBond);

        sqlQuery.append(getBondTableId(minimalBond));
        sqlQuery.append(".BONDTYPE='");
        sqlQuery.append(minimalBond.getAtom(0).getSymbol());
        sqlQuery.append(getBondCharacter(minimalBond));
        sqlQuery.append(minimalBond.getAtom(1).getSymbol());
        sqlQuery.append("' AND ");

        ArrayList<IBond> minimalBondNeighbours = getNeighbourBonds(query, minimalBond, foundBonds, nonSpanningTreeBonds, kruskal);
        minimalBondNeighbours = kruskal.getOrderBondList(minimalBondNeighbours);
        queue.addAll(minimalBondNeighbours);

        while (!queue.isEmpty()) {
            IBond currentBond = queue.poll();
            IAtom currentBondAtom1 = currentBond.getAtom(0);
            IAtom currentBondAtom2 = currentBond.getAtom(1);

            StringBuilder preventMirrorBondsSubquery = new StringBuilder();

            //TODO update SQL query
            for (IBond bond : query.getConnectedBondsList(currentBondAtom1)) {
                if (processedBonds.contains(bond)) {
                    sqlQuery.append(getBondTableId(currentBond));
                    sqlQuery.append(".ATOM1_ID = ");
                    sqlQuery.append(getBondTableId(bond));

                    preventMirrorBondsSubquery.append(getBondTableId(currentBond));
                    preventMirrorBondsSubquery.append(".ATOM2_ID != ");
                    preventMirrorBondsSubquery.append(getBondTableId(bond));

                    if (bond.getAtom(0).equals(currentBondAtom1)) {
                        sqlQuery.append(".ATOM1_ID");
                        preventMirrorBondsSubquery.append(".ATOM2_ID");
                    }
                    else {
                        sqlQuery.append(".ATOM2_ID");
                        preventMirrorBondsSubquery.append(".ATOM1_ID");
                    }

                    sqlQuery.append(" AND ");
                    preventMirrorBondsSubquery.append(" AND ");
                }
            }
            for (IBond bond : query.getConnectedBondsList(currentBondAtom2)) {
                if (processedBonds.contains(bond)) {
                    sqlQuery.append(getBondTableId(currentBond));
                    sqlQuery.append(".ATOM2_ID = ");
                    sqlQuery.append(getBondTableId(bond));

                    preventMirrorBondsSubquery.append(getBondTableId(currentBond));
                    preventMirrorBondsSubquery.append(".ATOM1_ID != ");
                    preventMirrorBondsSubquery.append(getBondTableId(bond));

                    if (bond.getAtom(0).equals(currentBondAtom2)) {
                        sqlQuery.append(".ATOM1_ID");
                        preventMirrorBondsSubquery.append(".ATOM2_ID");
                    }
                    else {
                        sqlQuery.append(".ATOM2_ID");
                        preventMirrorBondsSubquery.append(".ATOM1_ID");
                    }

                    sqlQuery.append(" AND ");
                    preventMirrorBondsSubquery.append(" AND ");
                }
            }

            //Add bond type
            sqlQuery.append(getBondTableId(currentBond));
            sqlQuery.append(".BONDTYPE='");
            sqlQuery.append(currentBondAtom1.getSymbol());
            sqlQuery.append(getBondCharacter(currentBond));
            sqlQuery.append(currentBondAtom2.getSymbol());
            sqlQuery.append("' AND ");

            //Prevent mirroring bonds
            sqlQuery.append(preventMirrorBondsSubquery.toString());


            //Add neigbours to queue
            ArrayList<IBond> neighbours;
            neighbours = getNeighbourBonds(query, currentBond, foundBonds, nonSpanningTreeBonds, kruskal);
            neighbours = kruskal.getOrderBondList(neighbours);
            queue.addAll(neighbours);

            processedBonds.add(currentBond);

            if (queue.isEmpty()) {
                nonSpanningTreeBonds = kruskal.getOrderBondList(nonSpanningTreeBonds);
                queue.addAll(nonSpanningTreeBonds);
                nonSpanningTreeBonds.clear();
            }
        }

        //Removing last AND
        sqlQuery.setLength(sqlQuery.length() - 4);

        try {
            Statement statement = connection.createStatement();
            ResultSet sqlResult = statement.executeQuery(sqlQuery.toString());

            while (sqlResult.next()) {
                result.add(sqlResult.getString("compound_id"));
            }
            sqlResult.close();
        } catch (Exception e) {
            System.out.println(e);
        }

        return result;
    }

    private String getBondTableId(IBond bond) {
        String id = bond.getID();
        return "b" + id;
    }

    private ArrayList<IBond> getNeighbourBonds(IAtomContainer query, IBond bond, HashSet<IBond> foundBonds, ArrayList<IBond> nonSpanningTreeBonds, Kruskal kruskal) {
        ArrayList<IBond> neighbours = getNeighbourBonds(query, bond.getAtom(0), foundBonds, nonSpanningTreeBonds, kruskal);
        neighbours.addAll(getNeighbourBonds(query, bond.getAtom(1), foundBonds, nonSpanningTreeBonds, kruskal));

        return neighbours;
    }

    private ArrayList<IBond> getNeighbourBonds(IAtomContainer query, IAtom atom, HashSet<IBond> foundBonds, ArrayList<IBond> nonSpanningTreeBonds, Kruskal kruskal) {
        ArrayList<IBond> neighbours = new ArrayList<>();

        for (IBond bond : query.getConnectedBondsList(atom)) {
            if (!foundBonds.contains(bond)) {
                if (kruskal.isBondInSpanningTree(bond)) {
                    neighbours.add(bond);
                }
                else {
                    nonSpanningTreeBonds.add(bond);
                }

                foundBonds.add(bond);
            }
        }

        return neighbours;
    }

    private void updateStatistics(String bond) {
        int bondCount = statistics.getOrDefault(bond, 0);
        statistics.put(bond, bondCount + 1);
    }

    public static char getBondCharacter(IBond bond) {
        if (bond.getOrder().equals(IBond.Order.SINGLE)){
            return '-';
        }
        if (bond.getOrder().equals(IBond.Order.DOUBLE)) {
            return '=';
        }
        if (bond.getOrder().equals(IBond.Order.TRIPLE)) {
            return '#';
        }
        if (bond.getOrder().equals(IBond.Order.QUADRUPLE)) {
            return '4';
        }
        if (bond.getOrder().equals(IBond.Order.QUINTUPLE)) {
            return '5';
        }
        if (bond.getOrder().equals(IBond.Order.SEXTUPLE)) {
            return '6';
        }

        return ' ';
    }
}
