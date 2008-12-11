package etomica.nbr;

import etomica.api.IAtomLeaf;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.INearestImageTransformer;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomSetSinglet;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.box.BoxAgentManager;
import etomica.box.BoxAgentSourceAtomManager;
import etomica.space.ISpace;
import etomica.units.Dimension;
import etomica.units.Length;
import etomica.util.Debug;

/**
 * Simple neighbor criterion based on distance moved by a leaf atom since
 * the last update.
 * @author andrew
 *
 */
public class CriterionSimple implements NeighborCriterion, AgentSource, java.io.Serializable {

	public CriterionSimple(ISimulation sim, ISpace _space, double interactionRange, double neighborRadius) {
		super();
        this.space = _space;
        dr = space.makeVector();
		this.interactionRange = interactionRange;
        neighborRadius2 = neighborRadius * neighborRadius;
        setSafetyFactor(0.4);
        boxAgentManager = new BoxAgentManager(new BoxAgentSourceAtomManager(this),sim,true);
	}
	
	public void setSafetyFactor(double f) {
		if (f <= 0.0 || f >= 0.5) throw new IllegalArgumentException("safety factor must be positive and less than 0.5");
		safetyFactor = f;
		double displacementLimit = (Math.sqrt(neighborRadius2) - interactionRange) * f;
		displacementLimit2 = displacementLimit * displacementLimit;
        r2MaxSafe = displacementLimit2 / (4.0*safetyFactor*safetyFactor);
	}
	
	public double getSafetyFactor() {
		return safetyFactor;
	}
	
	public void setNeighborRange(double r) {
		neighborRadius2 = r*r;
		double displacementLimit = (r - interactionRange) * safetyFactor;
		displacementLimit2 = displacementLimit * displacementLimit;
        r2MaxSafe = displacementLimit2 / (4.0*safetyFactor*safetyFactor);
	}
    
    public double getNeighborRange() {
        return Math.sqrt(neighborRadius2);
    }
    
    public Dimension getNeighborRangeDimension() {
        return Length.DIMENSION;
    }
    
    public void setInteractionRange(double newInteractionRange) {
        interactionRange = newInteractionRange;
        double displacementLimit = (Math.sqrt(neighborRadius2) - interactionRange) * safetyFactor;
        displacementLimit2 = displacementLimit * displacementLimit;
        r2MaxSafe = displacementLimit2 / (4.0*safetyFactor*safetyFactor);
    }
    
    public double getInteractionRange() {
        return interactionRange;
    }
    
    public Dimension getInteractionRangeDimension() {
        return Length.DIMENSION;
    }
	
	public boolean needUpdate(IAtomLeaf atom) {
        if (Debug.ON && interactionRange > Math.sqrt(neighborRadius2)) {
            throw new IllegalStateException("Interaction range ("+interactionRange+") must be less than neighborRange ("+Math.sqrt(neighborRadius2)+")");
        }
		r2 = ((IAtomPositioned)atom).getPosition().Mv1Squared((IVector)agentManager.getAgent(atom));
        if (Debug.ON && Debug.DEBUG_NOW && Debug.LEVEL > 1 && Debug.allAtoms(new AtomSetSinglet(atom))) {
            System.out.println("atom "+atom+" displacement "+r2+" "+((IAtomPositioned)atom).getPosition());
        }
		if (Debug.ON && Debug.DEBUG_NOW && r2 > displacementLimit2 / (4.0*safetyFactor*safetyFactor)) {
			System.out.println("atom "+atom+" exceeded safe limit ("+r2+" > "+displacementLimit2 / (4.0*safetyFactor*safetyFactor)+")");
			System.out.println("old position "+agentManager.getAgent(atom));
			System.out.println("new position "+((IAtomPositioned)atom).getPosition());
//            throw new RuntimeException("stop that");
		}
		return r2 > displacementLimit2;
	}

	public void setBox(IBox box) {
        nearestImageTransformer = box.getBoundary();
        agentManager = (AtomLeafAgentManager)boxAgentManager.getAgent(box);
	}
    
	public boolean unsafe() {
		if (Debug.ON && Debug.DEBUG_NOW && r2 > displacementLimit2 / (4.0*safetyFactor*safetyFactor)) {
			System.out.println("some atom exceeded safe limit ("+r2+" > "+displacementLimit2 / (4.0*safetyFactor*safetyFactor));
		}
		return r2 > r2MaxSafe;
	}

	public boolean accept(IAtomList pair) {
        dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(),((IAtomPositioned)pair.getAtom(0)).getPosition());
        nearestImageTransformer.nearestImage(dr);
        if (Debug.ON && neighborRadius2 < interactionRange*interactionRange) {
            throw new IllegalStateException("neighbor radius "+Math.sqrt(neighborRadius2)+" is less than interaction range "+interactionRange);
        }
		if (Debug.ON && Debug.DEBUG_NOW && ((Debug.LEVEL > 1 && Debug.anyAtom(pair)) || (Debug.LEVEL == 1 && Debug.allAtoms(pair)))) {
            double r2l = dr.squared(); 
			if (r2l < neighborRadius2 || (Debug.LEVEL > 1 && Debug.allAtoms(pair))) {
				System.out.println("Atom "+pair.getAtom(0)+" and "+pair.getAtom(1)+" are "+(r2l < neighborRadius2 ? "" : "not ")+"neighbors, r2="+r2l);
            }
		}
		return dr.squared() < neighborRadius2;
	}
	
	public void reset(IAtomLeaf atom) {
        ((IVector)agentManager.getAgent(atom)).E(((IAtomPositioned)atom).getPosition());
	}

    public Class getAgentClass() {
        return dr.getClass();
    }
    
    public Object makeAgent(IAtomLeaf atom) {
        return atom instanceof IAtomPositioned ? space.makeVector() : null;
    }
    
    public void releaseAgent(Object agent, IAtomLeaf atom) {}

    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    private double interactionRange, displacementLimit2, neighborRadius2;
	private final IVector dr;
    private INearestImageTransformer nearestImageTransformer;
	protected double safetyFactor;
	protected double r2, r2MaxSafe;
    private AtomLeafAgentManager agentManager;
    private final BoxAgentManager boxAgentManager;
}
