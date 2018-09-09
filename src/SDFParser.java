import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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


        while (reader.hasNext()) {
            IAtomContainer molecule = reader.next();
            System.out.println((String)molecule.getProperty(propertyID));
            if (molecule.getProperty(propertyID).equals("CHEMBL503870"))
            result.put(molecule.getProperty(propertyID), molecule);
            count++;
            if (count % 50000 == 0) {
                System.out.println(count);
            }
            if (count == 10) {
                break;
            }
        }

        System.out.println(count);

        return result;
    }
}
