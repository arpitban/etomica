package etomica.potential; 

import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.space.ISpace;

/**
 * Potential acting on a single atom or atom group.
 *
 * @author David Kofke
 */
public abstract class Potential1 extends Potential {
      
	protected IBoundary boundary;
	
    public Potential1(ISpace space) {
        super(1, space);
    }

    public void setBox(IBox box) {
    	boundary = box.getBoundary();
    }
    
    /**
     * Returns zero.
     */
    public double getRange() {
        return 0.0;
    }
    
    /**
     * Marker interface indicating that a one-body potential is an intramolecular
     * potential, and not, e.g., a potential of interaction with an external field.
     * This is useful when computing energy changes for molecule translations and
     * rotations, for which intramolecular contributions can be ignored.
     */
    public interface Intramolecular {}

}
