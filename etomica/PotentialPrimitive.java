package etomica;

import etomica.electrostatics.*;

/**
 * Potential for the primitive model of an electrolyte.
 * The primitive model is a simple Coulombic charge with a hard-sphere repulsion.
 * Charges are ascribed to the Atoms via their AtomType.electroType field
 * Hard-sphere diameter is defined by this potential
 */
public class P2Primitive extends Potential2 implements EtomicaElement {

    public String getVersion() {return "P2Primitive:01.07.08/"+Potential2SoftSpherical.VERSION;}

    private double sigma, sigmaSquared;
    private double cutoffRadius, cutoffRadiusSquared;
    private double cutoff;
    private double eLRC, pLRC;  //multipliers for long-range correction to energy and pressure, resp.

    public P2Primitive() {
        this(Simulation.instance, Default.ATOM_SIZE, Default.POTENTIAL_CUTOFF_FACTOR);
    }
    public P2Primitive(double sigma, double cutoff) {
        this(Simulation.instance, sigma, cutoff);
    }
    public P2Primitive(Simulation sim, double sigma, double cutoff) {
        super(sim);
        setSigma(sigma);
        setCutoff(cutoff);
        force = sim.space().makeVector();
    }
 
   /**
    * Returns primitive-model energy.
    * Return infinity if overlap is true, and zero if separation is greater than cutoff.
    */
    public double energy(AtomPair pair) {
        if(r2 < sigmaSquared) {return Double.MAX_VALUE;}
        else {
            double z1 = ((Monopole)pair.atom1.type.electroType()).getZ();
            double z2 = ((Monopole)pair.atom2.type.electroType()).getZ();
//            return z1*z2/Math.sqrt(r2);
            return -0.5*z1*z2*Math.log(r2);
        }
    }
    /** 
     * Force that atom2 exerts on atom1
     * This method has not been checked and may be incorrect
     */
    public Space.Vector force(AtomPair pair) {
        double r2 = pair.r2();
        if(r2 > cutoffRadiusSquared) {force.E(0.0);}
        else if(r2 < sigmaSquared) {force.E(Double.MAX_VALUE);}
        else {
            double z1 = ((Monopole)pair.atom1.type.electroType()).getZ();
            double z2 = ((Monopole)pair.atom2.type.electroType()).getZ();
            double c = z1*z2/Math.sqrt(r2);
            force.E(pair.dr());
            force.TE(-c/r2);
        }
        return force;
    }            
     
    /**
     * Accessor method for the size of the repulsive core of the primitive model
     */
    public double getSigma() {return sigma;}
    /**
     * Accessor method for the size of the repulsive core of the primitive model
     */
    public final void setSigma(double s) {
        sigma = s;
        sigmaSquared = s*s;
        setCutoff(cutoff);
    }
    public etomica.units.Dimension getSigmaDimension() {return etomica.units.Dimension.LENGTH;}

    /**
     * Accessor method for cutoff distance; divided by sigma
     * @return cutoff distance, divided by size parameter (sigma)
     */
    public double getCutoff() {return cutoff;}
    /**
     * Accessor method for cutoff distance; divided by sigma
     * @param rc cutoff distance, divided by size parameter (sigma)
     */
    public final void setCutoff(double rc) {  //argument is the cutoff radius divided by sigma
        cutoff = rc;
        cutoffRadius = sigma*cutoff;
        cutoffRadiusSquared = cutoffRadius*cutoffRadius;
        calculateLRC();
    }    
}
  