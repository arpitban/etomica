/*
 * Created on Jan 17, 2004
 */
package etomica.chem.models;

import etomica.Default;
import etomica.chem.Electrostatic;
import etomica.chem.Element;
import etomica.potential.P2LennardJones;
import etomica.potential.Potential;
import etomica.space.Space;

/**
 * @author zhaofang
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LennardJones extends ModelAtomic {
	
	private final double sigma, epsilon;
	
	public LennardJones(){
		this(Default.ATOM_SIZE, Default.POTENTIAL_WELL);
	}
	
	public LennardJones(double s, double e){
		sigma = s;
		epsilon = e;
	}
	public LennardJones(Element element, double s, double e) {
		this(element, s, e, null);
	}
	
	public LennardJones(Element element, double s, double e, Electrostatic electrostatic) {
		super(element, electrostatic);
		sigma = s;
		epsilon = e;
	}
	
	public Potential makePotential(Space space) {
		return new P2LennardJones(space, sigma, epsilon);
	}

	/**
	 * @return 
	 */
	public double getEpsilon() {
		return epsilon;
	}

	/**
	 * @return
	 */
	public double getSigma() {
		return sigma;
	}

}
