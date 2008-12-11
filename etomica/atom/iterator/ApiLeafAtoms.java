package etomica.atom.iterator;

import etomica.api.IBox;

/**
 * Iterator that returns all pairs that can be formed from all leaf atoms of a
 * given box.
 */
public class ApiLeafAtoms extends ApiIntraArrayList implements
        AtomsetIteratorBoxDependent {

    /**
     * Creates new pair iterator that requires reset() before beginning
     * iteration.
     */
    public ApiLeafAtoms() {
        super();
    }

    /**
     * Conditions iterator to return all leaf-atom pairs from the given box.
     * @throws a NullPointerException if the Box is null
     */
    public void setBox(IBox box) {
        setList(box.getLeafList());
    }

    private static final long serialVersionUID = 1L;
}
