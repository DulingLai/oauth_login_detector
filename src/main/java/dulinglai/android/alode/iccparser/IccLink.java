package dulinglai.android.alode.iccparser;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;

public class IccLink {

    private final SootClass fromC;
    private final SootMethod fromSM;
    private final Unit fromU;
    private final SootClass destinationC;
    private String exit_kind;

    public IccLink(SootClass fromC, SootMethod fromSm, Unit fromU, SootClass destinationC) {
        this.fromC = fromC;
        this.fromSM = fromSm;
        this.fromU = fromU;
        this.destinationC = destinationC;
    }

    public String toString() {
        return fromSM + " [" + fromU + "] " + destinationC;
    }

    public SootClass getFromC() { return fromC; }

    public SootMethod getFromSM() {
        return fromSM;
    }

    public Unit getFromU() {
        return fromU;
    }

    public String getExit_kind() {
        return exit_kind;
    }

    public void setExit_kind(String exit_kind) {
        this.exit_kind = exit_kind;
    }

    public SootClass getDestinationC() {
        return destinationC;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fromC == null) ? 0 : fromC.hashCode());
        result = prime * result + ((destinationC == null) ? 0 : destinationC.hashCode());
        result = prime * result + ((exit_kind == null) ? 0 : exit_kind.hashCode());
        result = prime * result + ((fromSM == null) ? 0 : fromSM.hashCode());
        result = prime * result + ((fromU == null) ? 0 : fromU.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IccLink other = (IccLink) obj;
        if (destinationC == null) {
            if (other.destinationC != null)
                return false;
        } else if (!destinationC.equals(other.destinationC))
            return false;
        if (exit_kind == null) {
            if (other.exit_kind != null)
                return false;
        } else if (!exit_kind.equals(other.exit_kind))
            return false;
        if (fromSM == null) {
            if (other.fromSM != null)
                return false;
        } else if (!fromSM.equals(other.fromSM))
            return false;
        if (fromU == null) {
            return other.fromU == null;
        } else return fromU.equals(other.fromU);
    }

}
