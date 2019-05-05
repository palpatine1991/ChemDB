package Utils;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.xmlcml.euclid.Int;
import uk.ac.ebi.beam.Graph;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphSerializer {
    static int minSize = 999999;
    static int maxSize = 0;
    static int vertexSum = 0;
    static int edgeSum = 0;
    static HashSet<String> vertexLabels = new HashSet<>();
    static HashSet<String> edgeLabels = new HashSet<>();

    public static void serializeDB(String fileName, HashMap<String, IAtomContainer> db) {

        try {
            FileWriter fwriter = new FileWriter(fileName + ".db");
            BufferedWriter writer = new BufferedWriter(fwriter);
            FileWriter recordsFwriter = new FileWriter(fileName + ".mapping");
            BufferedWriter recordsWriter = new BufferedWriter(recordsFwriter);

            int order = 0;

            for (Map.Entry<String, IAtomContainer> entry : db.entrySet()) {
                GraphSerializer.serializeGraph(writer, recordsWriter, entry.getKey(), entry.getValue(), order);

                int size = entry.getValue().getAtomCount();
                int edges = entry.getValue().getBondCount();

                GraphSerializer.edgeSum += edges;
                GraphSerializer.vertexSum += size;
                if (size == 1) {
                    System.out.println(entry.getKey());
                }
                if (size < GraphSerializer.minSize) {
                    GraphSerializer.minSize = size;

                }
                if (size > GraphSerializer.maxSize) GraphSerializer.maxSize = size;

                order++;
            }

            System.out.print("Min Size: ");
            System.out.println(GraphSerializer.minSize);
            System.out.print("Max Size: ");
            System.out.println(GraphSerializer.maxSize);
            System.out.print("Average Size: ");
            System.out.println(GraphSerializer.vertexSum / 100000);
            System.out.print("Average edge count: ");
            System.out.println(GraphSerializer.edgeSum / 100000);

            System.out.println(vertexLabels.size());
            System.out.println(edgeLabels.size());

            for(String label : vertexLabels) {
                System.out.println(label);
            }

            for(String label : edgeLabels) {
                System.out.println(label);
            }

            writer.close();
            recordsWriter.close();
        }
        catch (IOException e) {
            System.out.println("IO Exception");
        }
    }

    private static void serializeGraph(BufferedWriter dbWriter, BufferedWriter recordsWriter, String id, IAtomContainer graph, int order) throws IOException {
        GraphSerializer.printRecordMapping(recordsWriter, id, order);

        int atomOrder = 0;

        GraphSerializer.printGraphHeader(dbWriter, order);

        for (IAtom atom : graph.atoms()) {
            atom.setID(Integer.toString(atomOrder++));
            GraphSerializer.printGraphNode(dbWriter, atom);
            GraphSerializer.vertexLabels.add(atom.getSymbol());
        }

        for (IBond bond : graph.bonds()) {
            GraphSerializer.printGraphEdge(dbWriter, bond);
            GraphSerializer.edgeLabels.add(bond.getOrder().toString());
        }

    }

    private static void printRecordMapping(BufferedWriter recordsWriter, String id, int order) throws IOException {
        recordsWriter.write(Integer.toString(order));
        recordsWriter.write(" ");
        recordsWriter.write(id);
        recordsWriter.newLine();
    }

    private static void printGraphHeader(BufferedWriter writer, int order) throws IOException {
        writer.write("t # ");
        writer.write(Integer.toString(order));
        writer.newLine();
    }

    private static void printGraphNode(BufferedWriter writer, IAtom atom) throws IOException {
        writer.write("v ");
        writer.write(atom.getID());
        writer.write(" ");

        int atomNumber = atom.getAtomicNumber();
        int identificator = atomNumber * 2;
        if (atom.isAromatic()) {
            identificator -= 1;
        }

        writer.write(Integer.toString(identificator));
        writer.newLine();
    }

    private static void printGraphEdge(BufferedWriter writer, IBond bond) throws IOException {
        writer.write("e ");
        writer.write(bond.getAtom(0).getID());
        writer.write(" ");
        writer.write(bond.getAtom(1).getID());
        writer.write(" ");
        writer.write(GraphSerializer.getBondCharacter(bond));
        writer.newLine();
    }

    private static String getBondCharacter(IBond bond) throws IOException {
        if (bond.isAromatic()) {
            return "10";
        }
        if (bond.getOrder().equals(IBond.Order.SINGLE)){
            return "1";
        }
        if (bond.getOrder().equals(IBond.Order.DOUBLE)) {
            return "2";
        }
        if (bond.getOrder().equals(IBond.Order.TRIPLE)) {
            return "3";
        }
        if (bond.getOrder().equals(IBond.Order.QUADRUPLE)) {
            return "4";
        }
        if (bond.getOrder().equals(IBond.Order.QUINTUPLE)) {
            return "5";
        }
        if (bond.getOrder().equals(IBond.Order.SEXTUPLE)) {
            return "6";
        }

        throw new IOException("Unknown type of bond");
    }
}
