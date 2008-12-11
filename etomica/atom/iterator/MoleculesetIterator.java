package etomica.atom.iterator;

import etomica.api.IMoleculeList;

/**
 * Interface for classes that loop over a set of atoms. Permits
 * iteration via a next()!=null while loop (iterator returns
 * atoms to client) or via a call to allAtoms(AtomsetActive) (client gives
 * action to iterator).
 */

public interface MoleculesetIterator extends AtomsetIterator {
    
	/**
	 * Returns the next AtomSet iterate, or null if hasNext() is false.
	 */
    public IMoleculeList next();
}
