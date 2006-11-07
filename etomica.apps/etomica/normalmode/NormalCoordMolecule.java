package etomica.normalmode;

import java.io.Serializable;

import etomica.action.AtomActionTranslateTo;
import etomica.atom.Atom;
import etomica.space.Space;
import etomica.space.Vector;

/**
 * NormalCoordWrapper implementation for molecules.  The class takes
 * the first space.D values of u to be real space displacements of the molecule
 * center of mass from its nominal position.  Subclasses should add 
 * additional u values for intramolecular degrees of freedom.
 * @author Andrew Schultz
 */
public class NormalCoordMolecule implements NormalCoordMapper, Serializable {

    public NormalCoordMolecule(Space space) {
        this.space = space;
        work1 = space.makeVector();
        atomActionTranslateTo = new AtomActionTranslateTo(space);
    }
    
    public void setNumAtoms(int numAtoms) {
        nominalU = new double[numAtoms][getNormalDim()];
    }
    
    public int getNormalDim() {
        return space.D();
    }

    public void calcU(Atom atom, int atomCount, double[] u) {
        Vector pos = atom.type.getPositionDefinition().position(atom);
        for (int i=0; i<pos.D(); i++) {
            u[i] = pos.x(i) - nominalU[atomCount][i];
        }
    }

    public void initNominalU(Atom atom, int atomCount) {
        Vector pos = atom.type.getPositionDefinition().position(atom);
        for (int i=0; i<pos.D(); i++) {
            nominalU[atomCount][i] = pos.x(i);
        }
    }

    public void setToU(Atom atom, int atomCount, double[] u) {
        for (int i=0; i<space.D(); i++) {
            work1.setX(i, nominalU[atomCount][i] + u[i]);
        }
        atomActionTranslateTo.setDestination(work1);
        atomActionTranslateTo.actionPerformed(atom);
    }

    private static final long serialVersionUID = 1L;
    protected final Space space;
    protected final Vector work1;
    protected double[][] nominalU;
    protected final AtomActionTranslateTo atomActionTranslateTo;
}
