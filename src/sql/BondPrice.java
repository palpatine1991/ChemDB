package sql;

import org.openscience.cdk.interfaces.IBond;

public class BondPrice implements Comparable {

    IBond bond;
    int price;
    public boolean presentInSpanningTree = false;

    public BondPrice(IBond bond, int price) {
        this.bond = bond;
        this.price = price;
    }

    public int compareTo(Object other) {
        if (this.price == ((BondPrice) other).price)
            return 0;
        else if ((this.price) > ((BondPrice) other).price)
            return 1;
        else
            return -1;
    }
}
