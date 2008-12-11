package etomica.atom.iterator;

import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.atom.MoleculeArrayList;
import etomica.atom.MoleculeSetSinglet;

 /**
  * An atom iterator of the elements from an AtomArrayList (in proper
  * sequence).  Iterator will fail if element are added to or removed 
  * from list while iteration is proceeding.
  */
public class MoleculeIteratorArrayListSimple implements MoleculeIterator, java.io.Serializable {

    /**
     * Constructs new iterator with an empty list.
     */
 	public MoleculeIteratorArrayListSimple() {
 		this(new MoleculeArrayList());
 	}
    
    /**
     * Constructs new iterator set to iterate given list (upon reset).
     */
 	public MoleculeIteratorArrayListSimple(IMoleculeList atomList) {
 		list = atomList;
        atomSetSinglet = new MoleculeSetSinglet();
 	}
    
    /**
     * Sets the list for iteration.  Null value will result in a
     * NullPointerException.
     */
 	public void setList(IMoleculeList atomList) {
        list = atomList;
 	}
 	
    /**
     * Returns 1, indicating that this is an atom iterator.
     */
 	public int nBody() {
        return 1;
    }
    
    /**
     * Puts iterator in state in which hasNext is false.
     */
 	public void unset() {
        cursor = list.getMoleculeCount();
    }
 
    /**
     * Returns the next iterate and advances the iterator.
     */
 	public IMolecule nextMolecule() {
        if (cursor < list.getMoleculeCount()) {
            return list.getMolecule(cursor++);
        }
        return null;
 	}
 	
    /**
     * Same as nextAtom().
     */
 	public IMoleculeList next() {
        IMolecule atom = nextMolecule();
        if (atom == null) return null;
        atomSetSinglet.atom = atom;
 		return atomSetSinglet;
 	}
 
    /**
     * Returns the number of iterates that would be given by this iterator
     * if reset with the current list.
     */
 	public int size() {
 		return list.getMoleculeCount();
 	}

    /**
     * Puts iterator in state ready to begin iteration.
     */
 	public void reset() {
 		cursor = 0;
 	}
 	
    private static final long serialVersionUID = 2L;

    /**
     * Index of element to be returned by subsequent call to next.
     */
    protected int cursor = 0;

    protected IMoleculeList list;
    protected final MoleculeSetSinglet atomSetSinglet;
 }
