package etomica.atom;

import etomica.api.IAtomList;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomPositioned;
import etomica.api.IMolecule;
import etomica.api.IVector;

/**
 * Returns the position of the first child leaf atom.  Recurses to find
 * the first child leaf atom.
 */

public class AtomPositionFirstAtom implements IAtomPositionDefinition, java.io.Serializable {

    public IVector position(IMolecule atom) {
        IAtomList childList = atom.getChildList();
        if (childList.getAtomCount() == 0) {
            return null;
        }
        return ((IAtomPositioned)childList.getAtom(0)).getPosition();
    }
    

    private static final long serialVersionUID = 1L;
}
