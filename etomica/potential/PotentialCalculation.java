package etomica.potential;

import etomica.api.IAtomList;
import etomica.api.IPotentialAtomic;

/**
 * Class defining a particular action to be performed on a set of atoms using an
 * arbitrary potential.  Examples of such actions are summing the energy, 
 * computing forces on atoms, determining collision times, etc.
 * Concrete subclasses define these actions through implementation of the 
 * doCalculation(IAtomSet, IPotential) method, which should perform the
 * defined calculation on the atoms using the given potential.
 *
 * @see PotentialMaster
 * @see PotentialGroup
 */
public interface PotentialCalculation {
 	
	/**
	 * Method giving the specific calculation performed by this class.
	 * @param atoms IAtomSet the atom sets for which the calculation is performed.
	 * @param potential The potential used to apply the action defined by this class.
	 */
	public void doCalculation(IAtomList atoms, IPotentialAtomic potential);
	
}
