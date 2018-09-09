package GString;

public class NodeMatchCount implements Cloneable {
    public int specialAtomCount;
    public int specialBondCount;
    public int branchCount;

    public NodeMatchCount(int specialAtomCount, int specialBondCount, int branchCount) {
        this.specialAtomCount = specialAtomCount;
        this.specialBondCount = specialBondCount;
        this.branchCount = branchCount;
    }
    public NodeMatchCount() {
        this.specialAtomCount = 0;
        this.specialBondCount = 0;
        this.branchCount = 0;
    }

    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            assert false : "not supported clone";
        }

        return null;
    }
}
