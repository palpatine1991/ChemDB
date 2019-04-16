package shared;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectedComponents;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class SDFParser {
    String path;
    int count = 0;

    public SDFParser(String path) {
        this.path = path;
    }

    public HashMap<String, IAtomContainer> getDB(String propertyID) throws FileNotFoundException {
        File sdfFile = new File(this.path);
        IteratingSDFReader reader = new IteratingSDFReader(
                new FileInputStream(sdfFile), DefaultChemObjectBuilder.getInstance()
        );

        HashMap<String, IAtomContainer> result = new HashMap<>();

        Aromaticity aromaticity = shared.Constants.getAromaticityModel();

        while (reader.hasNext()) {
            IAtomContainer molecule = reader.next();
            try {
                aromaticity.apply(molecule);
            } catch(CDKException e) {
                System.out.println(e);
            }

            //Checking that graph is connected, otherwise we skip it
            int[][] adj = GraphUtil.toAdjList(molecule);
            ConnectedComponents cc = new ConnectedComponents(adj);
            int[] components = cc.components();

            boolean nonconnected = false;

            for (int v = 0; v < adj.length; v++) {
                if (components[v] > 1) {
                    nonconnected = true;
                    break;
                }
            }

            if (nonconnected) {
                continue;
            }

            //System.out.println((String)molecule.getProperty(propertyID));
            //if (molecule.getProperty(propertyID).equals("CHEMBL1179192"))
                result.put(molecule.getProperty(propertyID), molecule);
            count++;
            if (count % 50000 == 0) {
                System.out.println(count);
            }
            if (count == 500) {
                break;
            }
        }

        //System.out.println(count);

        return result;
    }
}
