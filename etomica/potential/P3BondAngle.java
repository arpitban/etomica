package etomica.potential;

import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.INearestImageTransformer;
import etomica.api.IVector;
import etomica.space.Space;
import etomica.units.Angle;
import etomica.units.Dimension;
import etomica.units.Energy;

/**
 * Simple 3-body soft bond-angle potential 
 * @author andrew
 */
public class P3BondAngle extends Potential {

    public P3BondAngle(Space space) {
        super(3, space);
        dr12 = space.makeVector();
        dr23 = space.makeVector();
        setAngle(Math.PI);
    }

    public void setBox(IBox box) {
        nearestImageTransformer = box.getBoundary();
    }

    public double energy(IAtomSet atomSet) {
        IAtomPositioned atom0 = (IAtomPositioned)atomSet.getAtom(0);
        IAtomPositioned atom1 = (IAtomPositioned)atomSet.getAtom(1);
        IAtomPositioned atom2 = (IAtomPositioned)atomSet.getAtom(2);
        dr12.Ev1Mv2(atom1.getPosition(),atom0.getPosition());
        dr23.Ev1Mv2(atom2.getPosition(),atom1.getPosition());
        nearestImageTransformer.nearestImage(dr12);
        nearestImageTransformer.nearestImage(dr23);
        double costheta = -dr12.dot(dr23)/Math.sqrt(dr12.squared()*dr23.squared());
        double dtheta;
        // machine precision can give us numbers with magnitudes slightly greater than 1
        if (costheta > 1) {
            dtheta = 0;
        }
        else if (costheta < -1) {
            dtheta = Math.PI;
        }
        else {
            dtheta = Math.acos(costheta);
        }
        dtheta -= angle;
        return 0.5*epsilon*dtheta*dtheta;
    }

    /**
     * Sets the nominal bond angle (in radians)
     */
    public void setAngle(double newAngle) {
        angle = newAngle;
    }
    
    /**
     * Returns the nominal bond angle (in radians)
     */
    public double getAngle() {
        return angle;
    }
    
    public Dimension getAngleDimension() {
        return Angle.DIMENSION;
    }

    /**
     * Sets the characteristic energy of the potential
     */
    public void setEpsilon(double newEpsilon) {
        epsilon = newEpsilon;
    }
    
    /**
     * Returns the characteristic energy of the potential
     */
    public double getEpsilon() {
        return epsilon;
    }
    
    public Dimension getEpsilonDimension() {
        return Energy.DIMENSION;
    }
    
    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }

    protected final IVector dr12, dr23;
    protected INearestImageTransformer nearestImageTransformer;
    protected double angle;
    protected double epsilon;
    private static final long serialVersionUID = 1L;
}
