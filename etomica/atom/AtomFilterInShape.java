package etomica.atom;

import etomica.api.IAtom;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomPositioned;
import etomica.api.IMolecule;
import etomica.math.geometry.Shape;


/**
 * Filter that accepts atom if it is inside a specified Shape instance.
 * Position of atom is determined by an AtomPositionDefinition, which
 * if unspecified defaults for each atom to that given by the atom's
 * type. 
 *
 * @author David Kofke
 */

public class AtomFilterInShape implements AtomFilter, java.io.Serializable {

    /**
     * Create filter in which position definition is given by atom's type.
     */
    public AtomFilterInShape(Shape shape) {
        super();
        this.shape = shape;
    }

    /**
     * Returns true if the atom's position is inside the shape.
     */
    public boolean accept(IAtom atom) {
        if (atom instanceof IAtomPositioned) {
            return shape.contains(((IAtomPositioned)atom).getPosition());
        }
        if(positionDefinition == null) {
            return shape.contains(((IMolecule)atom).getType().getPositionDefinition().position((IMolecule)atom));
        }
        return shape.contains(positionDefinition.position((IMolecule)atom));
    }

    /**
     * @return Returns the shape.
     */
    public Shape getShape() {
        return shape;
    }
    /**
     * @param shape The shape to set.
     */
    public void setShape(Shape shape) {
        this.shape = shape;
    }
    /**
     * @return Returns the positionDefinition.
     */
    public IAtomPositionDefinition getPositionDefinition() {
        return positionDefinition;
    }
    /**
     * Sets position definition.  If given null, positionDefinition from
     * each atom's type will be used.
     * @param positionDefinition The positionDefinition to set.
     */
    public void setPositionDefinition(IAtomPositionDefinition positionDefinition) {
        this.positionDefinition = positionDefinition;
    }
    
    private static final long serialVersionUID = 1L;
    private Shape shape;
    private IAtomPositionDefinition positionDefinition;
}
