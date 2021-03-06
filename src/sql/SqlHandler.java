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
    int bondId = 0;

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
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public void clearDB() {
        try {
            Statement statement = connection.createStatement();
            statement.executeQuery("delete from bonds");
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
            if (atom.isAromatic()) {
                atom.setSymbol(atom.getSymbol().toLowerCase());
            }
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
            insertQuery.append("','");
            insertQuery.append(Integer.toString(bondId));
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
            insertQuery.append("','");
            insertQuery.append(Integer.toString(bondId++));
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

        for (IAtom atom : query.atoms()) {
            if (atom.isAromatic()) {
                atom.setSymbol(atom.getSymbol().toLowerCase());
            }
        }

        //Bonds added to the queue
        HashSet<IBond> foundBonds = new HashSet<>();
        //Bonds added to SQL query
        HashSet<IBond> processedBonds = new HashSet<>();

        Kruskal kruskal = new Kruskal(query, statistics);

        sqlQuery.append("SELECT DISTINCT b0.compound_id FROM ");

        int bondCount = 0;

        for (IBond bond : query.bonds()) {
            sqlQuery.append("BONDS ");
            sqlQuery.append(getBondTableId(bond));
            sqlQuery.append(", ");
            bondCount++;
        }

        //We need to remove last comma
        sqlQuery.setLength(sqlQuery.length() - 2);
        sqlQuery.append(" WHERE ");


        //We need to add this for making sure that matched edges are distinct
        //TODO measure how much it slows the queries down
        for (int i = 0; i < bondCount; i++) {
            for (int j = i + 1; j < bondCount; j++) {
                sqlQuery.append("b" + i + ".BOND_ID != b" + j + ".BOND_ID");
                sqlQuery.append(" AND ");
            }
        }

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

        ArrayList<String> uniqueAtoms = new ArrayList<>();
        uniqueAtoms.add(getBondTableId(minimalBond) + ".ATOM1_ID");
        uniqueAtoms.add(getBondTableId(minimalBond) + ".ATOM2_ID");

        while (!queue.isEmpty()) {
            IBond currentBond = queue.poll();
            IAtom currentBondAtom1 = currentBond.getAtom(0);
            IAtom currentBondAtom2 = currentBond.getAtom(1);

            ArrayList<String> atom1ConnectedAtoms = new ArrayList<>();
            ArrayList<String> atom2ConnectedAtoms = new ArrayList<>();

            for (IBond bond : query.getConnectedBondsList(currentBondAtom1)) {
                if (processedBonds.contains(bond)) {
                    sqlQuery.append(getBondTableId(currentBond));
                    sqlQuery.append(".ATOM1_ID = ");
                    sqlQuery.append(getBondTableId(bond));

                    if (bond.getAtom(0).equals(currentBondAtom1)) {
                        sqlQuery.append(".ATOM1_ID");
                        atom1ConnectedAtoms.add(getBondTableId(bond) + ".ATOM1_ID");
                    }
                    else {
                        sqlQuery.append(".ATOM2_ID");
                        atom1ConnectedAtoms.add(getBondTableId(bond) + ".ATOM2_ID");
                    }

                    sqlQuery.append(" AND ");
                }
            }
            for (IBond bond : query.getConnectedBondsList(currentBondAtom2)) {
                if (processedBonds.contains(bond)) {
                    sqlQuery.append(getBondTableId(currentBond));
                    sqlQuery.append(".ATOM2_ID = ");
                    sqlQuery.append(getBondTableId(bond));

                    if (bond.getAtom(0).equals(currentBondAtom2)) {
                        sqlQuery.append(".ATOM1_ID");
                        atom2ConnectedAtoms.add(getBondTableId(bond) + ".ATOM1_ID");
                    }
                    else {
                        sqlQuery.append(".ATOM2_ID");
                        atom2ConnectedAtoms.add(getBondTableId(bond) + ".ATOM2_ID");
                    }

                    sqlQuery.append(" AND ");
                }
            }


            //Add bond type
            sqlQuery.append(getBondTableId(currentBond));
            sqlQuery.append(".BONDTYPE='");
            sqlQuery.append(currentBondAtom1.getSymbol());
            sqlQuery.append(getBondCharacter(currentBond));
            sqlQuery.append(currentBondAtom2.getSymbol());
            sqlQuery.append("' AND ");

            if (atom1ConnectedAtoms.size() == 0) {
                for (String id : uniqueAtoms) {
                    boolean connected = false;
                    for (String atom1Id : atom1ConnectedAtoms) {
                        if (id.equals(atom1Id)) {
                            connected = true;
                            break;
                        }
                    }

                    if (!connected) {
                        sqlQuery.append(getBondTableId(currentBond) + ".ATOM1_ID");
                        sqlQuery.append(" != ");
                        sqlQuery.append(id);
                        sqlQuery.append(" AND ");
                    }
                }
                uniqueAtoms.add(getBondTableId(currentBond) + ".ATOM1_ID");
            }
            if (atom2ConnectedAtoms.size() == 0) {
                for (String id : uniqueAtoms) {
                    boolean connected = false;

                    for (String atom2Id : atom2ConnectedAtoms) {
                        if (id.equals(atom2Id)) {
                            connected = true;
                            break;
                        }
                    }

                    if (!connected) {
                        sqlQuery.append(getBondTableId(currentBond) + ".ATOM2_ID");
                        sqlQuery.append(" != ");
                        sqlQuery.append(id);
                        sqlQuery.append(" AND ");
                    }
                }

                uniqueAtoms.add(getBondTableId(currentBond) + ".ATOM2_ID");
            }


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

        System.out.println(sqlQuery.toString());

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
        if (bond.isAromatic()) {
            return '~';
        }
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
