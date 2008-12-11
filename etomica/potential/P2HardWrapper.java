package etomica.potential;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * Hard potential class that wraps another hard potential.
 *
 * @author Andrew Schultz
 */
 public class P2HardWrapper implements PotentialHard {

    public P2HardWrapper(ISpace space, PotentialHard potential) {
        this.space = space;
        wrappedPotential = potential;
    }

    public double energy(IAtomList atoms) {
        return wrappedPotential.energy(atoms);
    }

    public int nBody() {
        return wrappedPotential.nBody();
    }

    public void setBox(IBox box) {
        wrappedPotential.setBox(box);
    }
    
    public PotentialHard getWrappedPotential() {
        return wrappedPotential;
    }

    public void setWrappedPotential(PotentialHard newWrappedPotential) {
        wrappedPotential = newWrappedPotential;
    }

    public double getRange() {
        return wrappedPotential.getRange();
    }
    
    public void bump(IAtomList atoms, double falseTime) {
        wrappedPotential.bump(atoms, falseTime);
    }

    public double collisionTime(IAtomList atoms, double falseTime) {
        return wrappedPotential.collisionTime(atoms, falseTime);
    }

    public double energyChange() {
        return wrappedPotential.energyChange();
    }

    public double lastCollisionVirial() {
        return wrappedPotential.lastCollisionVirial();
    }

    public Tensor lastCollisionVirialTensor() {
        return wrappedPotential.lastCollisionVirialTensor();
    }

    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    protected PotentialHard wrappedPotential;
}
