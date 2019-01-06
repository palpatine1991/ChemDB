import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;

public final class Constants {
    private static ElectronDonation model = ElectronDonation.daylight();
    private static CycleFinder cycles = Cycles.all(15);

    public static Aromaticity getAromaticityModel() {
        return new Aromaticity(model, cycles);
    }
}
