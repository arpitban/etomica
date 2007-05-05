package etomica.virial;

import etomica.action.AtomAction;
import etomica.atom.IAtomGroup;
import etomica.atom.IAtomPositioned;
import etomica.integrator.mcmove.MCMoveRotateMolecule3D;
import etomica.phase.Phase;
import etomica.potential.PotentialMaster;
import etomica.space.IVector;
import etomica.util.IRandom;

public class MCMoveClusterRotateMolecule3D extends MCMoveRotateMolecule3D {

    public MCMoveClusterRotateMolecule3D(PotentialMaster potentialMaster,
            IRandom random) {
        super(potentialMaster, random);
        weightMeter = new MeterClusterWeight(potential);
        setName("MCMoveClusterMolecule");
    }
    
    public void setPhase(Phase p) {
        super.setPhase(p);
        weightMeter.setPhase(p);
        oldPositions = new IVector[molecule.getChildList().size()-1];
        for (int j=0; j<oldPositions.length; j++) {
            oldPositions[j] = p.getSpace().makeVector();
        }
    }

    public boolean doTrial() {
        if(phase.moleculeCount()==1) {molecule = null; return false;}
            
        molecule = (IAtomGroup)moleculeSource.getAtom();
        while (molecule.getIndex() == 0) {
            molecule = (IAtomGroup)moleculeSource.getAtom();
        }
        uOld = weightMeter.getDataAsScalar();
        
        double dTheta = (2*random.nextDouble() - 1.0)*stepSize;
        rotationTensor.setAxial(random.nextInt(3),dTheta);

        leafAtomIterator.setRootAtom(molecule);
        leafAtomIterator.reset();
        IAtomPositioned first = (IAtomPositioned)leafAtomIterator.nextAtom();
        int j=0;
        for (IAtomPositioned a = (IAtomPositioned)leafAtomIterator.nextAtom(); a != null;
             a = (IAtomPositioned)leafAtomIterator.nextAtom()) {
            oldPositions[j++].E(a.getPosition());
        }
        leafAtomIterator.reset();
        r0.E(first.getPosition());
        doTransform();
            
        if (trialCount-- == 0) {
            relaxAction.setAtom(molecule);
            relaxAction.actionPerformed();
            trialCount = relaxInterval;
        }

        uNew = Double.NaN;
        ((PhaseCluster)phase).trialNotify();
        return true;
    }
    
    public double getB() {
        return 0.0;
    }
    
    public double getA() {
        uNew = weightMeter.getDataAsScalar();
        return (uOld==0.0) ? Double.POSITIVE_INFINITY : uNew/uOld;
    }
    
    public void acceptNotify() {
        super.acceptNotify();
        ((PhaseCluster)phase).acceptNotify();
    }
    
    public void rejectNotify() {
        super.rejectNotify();
        ((PhaseCluster)phase).rejectNotify();
    }
    
    public void setRelaxAction(AtomAction action) {
        relaxAction = action;
    }
    
    private static final long serialVersionUID = 1L;
    private final MeterClusterWeight weightMeter;
    protected int trialCount, relaxInterval = 100;
    protected AtomAction relaxAction;
    private IVector[] oldPositions;
}
