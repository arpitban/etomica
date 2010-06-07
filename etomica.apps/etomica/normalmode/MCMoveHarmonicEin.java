package etomica.normalmode;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IRandom;
import etomica.api.IVector;
import etomica.api.IVectorMutable;

/**
 * MC move whose purpose in life is to sample a system whose energy is a
 * combination of normal mode contributions and Einstein crystal
 * contributions.  Since both are harmonic, each configuration can be
 * independent.
 *
 * @author Andrew Schultz
 */
public class MCMoveHarmonicEin extends MCMoveHarmonic {

    public MCMoveHarmonicEin(IRandom random) {
        super(random);
    }

    /**
     * Sets the Einstein spring constant
     */
    public void setAlphaEin(double newAlpha) {
        alpha = newAlpha;
    }
    
    public double getAlpha() {
        return alpha;
    }

    /**
     * Sets the fraction of the systems energy from the normal mode
     * contribution.
     */
    public void setFrac(double newFrac) {
        frac = newFrac;
    }
    
    public double getFrac() {
        return frac;
    }
    
    public boolean doTrial() {
        if (!super.doTrial()) {
            return false;
        }
        
        IAtomList atomList = box.getLeafList();
        double sqrtFrac = Math.sqrt(frac);
        double einFac = Math.sqrt((1-frac)*temperature/alpha);
        lastU = 0;
        for (int i=0; i<atomList.getAtomCount(); i++) {
            IAtom a = atomList.getAtom(i);
            IVectorMutable p = a.getPosition();
            IVector site = coordinateDefinition.getLatticePosition(a);
            p.ME(site);
            for (int k=0; k<p.getD(); k++) {
                double r = random.nextGaussian();
                lastU += 0.5*r*r;
                p.setX(k, sqrtFrac*p.getX(k) + einFac * random.nextGaussian() );
            }
            p.PE(site);
        }
        lastU *= temperature;
        
        return true;
    }
    
    protected double alpha;
    protected double frac;
    protected double lastU;
}
