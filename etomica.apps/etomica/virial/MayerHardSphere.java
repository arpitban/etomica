package etomica.virial;

import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.space.ISpace;

/**
 * @author kofke
 *
 * Hard-sphere Mayer function.  -1 if r < sigma; 0 otherwise
 */
public class MayerHardSphere extends MayerFunctionSpherical {

    private static final long serialVersionUID = 1L;
    private double sigma, sigma2;
    private IPotential potential;
    /**
     * Constructor for MayerHardSphere.
     */
    public MayerHardSphere(ISpace _space) {
        this(_space, 1.0);
    }
    public MayerHardSphere(ISpace _space, double sigma) {
        super(_space);
        setSigma(sigma);
    }

    /**
     * @see etomica.virial.MayerFunctionSpherical#f(etomica.AtomPair)
     */
    public double f(double r2, double beta) {
        return (r2<sigma2) ? -1.0 : 0.0;
    }

    /**
     * Returns the HS diameter.
     * @return double
     */
    public double getSigma() {
        return sigma;
    }

    /**
     * Sets the HS diameter.
     * @param sigma The sigma to set
     */
    public void setSigma(double sigma) {
        this.sigma = sigma;
        sigma2 = sigma*sigma;
    }

    public IPotential getPotential() {
        return potential;
    }

    public void setBox(IBox newBox) {
    }
}
