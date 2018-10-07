package sql;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

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
            statement.executeQuery("delete from bonds");
        } catch(Exception e) {
            System.out.println(e);
        }
    }
    public void commitInsert() {
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
            //Inserting bonds in default order
            insertQuery.append("INTO BONDS VALUES (");
            insertQuery.append(bond.getAtom(0).getID());
            insertQuery.append(",");
            insertQuery.append(bond.getAtom(1).getID());
            insertQuery.append(",'");
            insertQuery.append(bond.getAtom(0).getSymbol());
            insertQuery.append(getBondCharacter(bond));
            insertQuery.append(bond.getAtom(1).getSymbol());
            insertQuery.append("','");
            insertQuery.append((String)molecule.getProperty("chembl_id"));
            insertQuery.append("') INTO BONDS VALUES (");

            //Inserting bonds in reversed order
            insertQuery.append(bond.getAtom(1).getID());
            insertQuery.append(",");
            insertQuery.append(bond.getAtom(0).getID());
            insertQuery.append(",'");
            insertQuery.append(bond.getAtom(1).getSymbol());
            insertQuery.append(getBondCharacter(bond));
            insertQuery.append(bond.getAtom(0).getSymbol());
            insertQuery.append("','");
            insertQuery.append((String)molecule.getProperty("chembl_id"));
            insertQuery.append("')");

            insertQuerySize += 2;
        }

        if (insertQuerySize > insertQueryLimit) {
            commitInsert();
        }
    }



    public void test() {
        try {
            // Get the JDBC driver name and version
            DatabaseMetaData dbmd = connection.getMetaData();
            System.out.println("Driver Name: " + dbmd.getDriverName());
            System.out.println("Driver Version: " + dbmd.getDriverVersion());
            // Print some connection properties
            System.out.println("Default Row Prefetch Value is: " +
                    connection.getDefaultRowPrefetch());
            System.out.println("Database Username is: " + connection.getUserName());
            System.out.println();
            // Perform a database operation

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select b1.compount_id from SYS.bonds b1, SYS.bonds b2 where b1.bondtype = 'C-C' AND b2.atom1_id = b1.atom2_id AND b2.bondtype = 'C=C'");
            System.out.println("FIRST_NAME" + "  " + "LAST_NAME");
            System.out.println("---------------------");
            while (resultSet.next()) {
                System.out.println(resultSet.getString(1));
            }
        }
        catch (Exception e) {
            System.out.print(e);
        }
    }

    private char getBondCharacter(IBond bond) {
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