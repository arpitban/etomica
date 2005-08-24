package etomica.atom;

import etomica.action.AtomActionAdapter;
import etomica.action.AtomGroupAction;
import etomica.data.Data;
import etomica.data.DataInfo;
import etomica.data.DataSource;
import etomica.data.types.DataVector;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.units.Dimension;

/**
 * Calculates the geometric center over a set of atoms. The position of all
 * atoms passed to the actionPerformed are accumulated and used to compute their
 * center (unweighted by mass). Calculated center is obtained via the getData
 * method, which returns an array with the elements of the calculated center
 * vector, or via the getCenter method, which returns a vector with the
 * calculated center. Calculation is zeroed via the reset method. <br>
 * A typical use of this class would have it passed to the allAtoms method of an
 * iterator, or wrapped in an AtomGroupAction to calculate the geometric center
 * of the atoms in an atom group.
 * 
 * @author David Kofke
 */

public class AtomPositionGeometricCenter extends AtomActionAdapter implements DataSource,
        AtomPositionDefinition {

    public AtomPositionGeometricCenter(Space space) {
        vectorSum = space.makeVector();
        data = new DataVector(space, "Geometric Center", Dimension.LENGTH);
        myAction = new MyAction(vectorSum);
        groupWrapper = new AtomGroupAction(myAction);
    }

    public DataInfo getDataInfo() {
        return data.getDataInfo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see etomica.action.AtomAction#actionPerformed(etomica.Atom)
     */
    public void actionPerformed(Atom a) {
        groupWrapper.actionPerformed(a);
    }

    /**
     * Returns the center of mass (calculated so far) as an array. Does not
     * reset COM sums. Implementation of DataSource interface.
     */
    public Data getData() {
        getCenter();
        return data;
    }

    /**
     * Returns the center of mass (calculated so far) as a vector. Does not
     * reset center-position sums.
     */
    public Vector getCenter() {
        data.x.Ea1Tv1(1.0 / myAction.nAtoms, vectorSum);
        return data.x;
    }

    //AtomPositionDefinition interface implementation
    public Vector position(Atom atom) {
        reset();
        actionPerformed(atom);
        return getCenter();
    }

    /**
     * Sets all accumulation sums to zero, readying for new center-of-mass
     * calculation
     */
    public void reset() {
        vectorSum.E(0.0);
        myAction.nAtoms = 0;
    }

    private static class MyAction extends AtomActionAdapter {
        public MyAction(Vector v) {
            vectorSum = v;
            nAtoms = 0;
        }
        
        public void actionPerformed(Atom a) {
            vectorSum.PE(a.coord.position());
            nAtoms++;
        }
        
        private Vector vectorSum;
        public int nAtoms;
    }

    private final DataVector data;
    private final Vector vectorSum;
    private AtomGroupAction groupWrapper;
    private MyAction myAction;
}