package etomica.potential;

import etomica.atom.AtomLeaf;
import etomica.atom.AtomPair;
import etomica.atom.AtomSet;
import etomica.phase.Phase;
import etomica.space.Coordinate;
import etomica.space.NearestImageTransformer;
import etomica.space.Space;
import etomica.space.Vector;

/**
 * Methods for a hard (impulsive), spherically-symmetric pair potential.
 * Subclasses must provide a concrete definition for the energy (method u(double)).
 */

public abstract class Potential2HardSpherical extends Potential2 implements PotentialHard, Potential2Spherical {
   
	public Potential2HardSpherical(Space space) {
	    super(space);
        dr = space.makeVector();
	}
	
	/**
    * The pair energy u(r^2) with no truncation applied.
    * @param the square of the distance between the particles.
    */
    public abstract double u(double r2);

    /**
     * Energy of the pair as given by the u(double) method, with application
     * of any PotentialTruncation that may be defined for the potential.  This
     * does not take into account any false positioning that the Integrator may
     * be using.
     */
    public double energy(AtomSet pair) {
        Coordinate coord0 = (Coordinate)((AtomLeaf)((AtomPair)pair).atom0).coord;
        Coordinate coord1 = (Coordinate)((AtomLeaf)((AtomPair)pair).atom1).coord;

        dr.Ev1Mv2(coord1.position(), coord0.position());
        nearestImageTransformer.nearestImage(dr);
        return u(dr.squared());
    }
    
    public void setPhase(Phase phase) {
        nearestImageTransformer = phase.getBoundary();
    }

    protected final Vector dr;
    protected NearestImageTransformer nearestImageTransformer;
}
