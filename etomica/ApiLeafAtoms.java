/*
 * History
 * Created on Sep 15, 2004 by kofke
 */
package etomica;

/**
 * Iterator that returns all pairs that can be formed from all
 * leaf atoms of a given phase.  Wraps an ApiListSimple instance.
 */
public class ApiLeafAtoms extends AtomsetIteratorAdapter implements
		AtomsetIteratorPhaseDependent {

	/**
	 * Creates new pair iterator that requires reset() before
	 * beginning iteration.
	 */
	public ApiLeafAtoms() {
		super(new ApiListSimple());
		apiList = (ApiListSimple)iterator;
	}

	/**
	 * Conditions iterator to return all leaf-atom pairs from
	 * the given phase.
	 */
	public void setPhase(Phase phase) {
		apiList.setList(phase.speciesMaster().atomList);
	}

	private final ApiListSimple apiList;
}
