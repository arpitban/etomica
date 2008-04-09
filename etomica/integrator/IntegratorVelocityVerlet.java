package etomica.integrator;

import java.io.Serializable;

import etomica.EtomicaInfo;
import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomSetSinglet;
import etomica.atom.IAtomKinetic;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.atom.iterator.IteratorDirective;
import etomica.exception.ConfigurationOverlapException;
import etomica.potential.PotentialCalculationForcePressureSum;
import etomica.potential.PotentialCalculationForceSum;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.util.Debug;

public class IntegratorVelocityVerlet extends IntegratorMD implements AgentSource {

    private static final long serialVersionUID = 2L;
    protected PotentialCalculationForceSum forceSum;;
    private final IteratorDirective allAtoms;
    protected final Tensor pressureTensor;
    protected final Tensor workTensor;
    
    protected AtomLeafAgentManager agentManager;

    public IntegratorVelocityVerlet(ISimulation sim, IPotentialMaster potentialMaster, ISpace _space) {
        this(potentialMaster, sim.getRandom(), 0.05, 1.0, _space);
    }
    
    public IntegratorVelocityVerlet(IPotentialMaster potentialMaster, IRandom random,
            double timeStep, double temperature, ISpace _space) {
        super(potentialMaster,random,timeStep,temperature, _space);
        // if you're motivated to throw away information earlier, you can use 
        // PotentialCalculationForceSum instead.
        forceSum = new PotentialCalculationForcePressureSum(potentialMaster.getSpace());
        allAtoms = new IteratorDirective();
        // allAtoms is used only for the force calculation, which has no LRC
        // but we're also calculating the pressure tensor, which does have LRC.
        // things deal with this OK.
        allAtoms.setIncludeLrc(true);
        pressureTensor = potentialMaster.getSpace().makeTensor();
        workTensor = potentialMaster.getSpace().makeTensor();
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Molecular dynamics using velocity Verlet integration algorithm");
        return info;
    }
    
    public void setForceSum(PotentialCalculationForceSum pc){
        forceSum = pc;
        if(box != null){
            forceSum.setAgentManager(agentManager);
        }
        
    }
    
    public void setBox(IBox p) {
        if (box != null) {
            // allow agentManager to de-register itself as a BoxListener
            agentManager.dispose();
        }
        super.setBox(p);
        agentManager = new AtomLeafAgentManager(this,p);
        forceSum.setAgentManager(agentManager);
    }
    
//--------------------------------------------------------------
// steps all particles across time interval tStep

    // assumes one box
    public void doStepInternal() {
        super.doStepInternal();
        if (Debug.ON && Debug.DEBUG_NOW) {
            IAtomSet pair = Debug.getAtoms(box);
            if (pair != null) {
                IVector dr = space.makeVector();
                dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(), ((IAtomPositioned)pair.getAtom(0)).getPosition());
                System.out.println(pair+" dr "+dr);
            }
        }
        IAtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
            MyAgent agent = (MyAgent)agentManager.getAgent(a);
            IVector r = a.getPosition();
            IVector v = a.getVelocity();
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(a))) {
                System.out.println("first "+a+" r="+r+", v="+v+", f="+agent.force);
            }
            v.PEa1Tv1(0.5*timeStep*((IAtomTypeLeaf)a.getType()).rm(),agent.force);  // p += f(old)*dt/2
            r.PEa1Tv1(timeStep,v);         // r += p*dt/m
        }

        forceSum.reset();
        //Compute forces on each atom
        potential.calculate(box, allAtoms, forceSum);
        
        if(forceSum instanceof PotentialCalculationForcePressureSum){
            pressureTensor.E(((PotentialCalculationForcePressureSum)forceSum).getPressureTensor());
        }
        
        //Finish integration step
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic)leafList.getAtom(iLeaf);
//            System.out.println("force: "+((MyAgent)a.ia).force.toString());
            IVector velocity = a.getVelocity();
            workTensor.Ev1v2(velocity,velocity);
            workTensor.TE(((IAtomTypeLeaf)a.getType()).getMass());
            pressureTensor.PE(workTensor);
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(a))) {
                System.out.println("second "+a+" v="+velocity+", f="+((MyAgent)agentManager.getAgent(a)).force);
            }
            velocity.PEa1Tv1(0.5*timeStep*((IAtomTypeLeaf)a.getType()).rm(),((MyAgent)agentManager.getAgent(a)).force);  //p += f(new)*dt/2
        }
        
        pressureTensor.TE(1/box.getBoundary().volume());

        if(isothermal) {
            doThermostat();
        }
    }

    /**
     * Returns the pressure tensor based on the forces calculated during the
     * last time step.
     */
    public Tensor getPressureTensor() {
        return pressureTensor;
    }
    
    public void reset() throws ConfigurationOverlapException{
        if(!initialized) return;
        
        super.reset();
        if (Debug.ON && Debug.DEBUG_NOW) {
            IAtomSet pair = Debug.getAtoms(box);
            if (pair != null) {
                IVector dr = space.makeVector();
                dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(), ((IAtomPositioned)pair.getAtom(0)).getPosition());
                System.out.println(pair+" dr "+dr);
            }
        }

        forceSum.reset();
        potential.calculate(box, allAtoms, forceSum);
    }
              
//--------------------------------------------------------------
    
    public Class getAgentClass() {
        return MyAgent.class;
    }

    public final Object makeAgent(IAtom a) {
        return new MyAgent(potential.getSpace());
    }
    
    public void releaseAgent(Object agent, IAtom atom) {}
            
    public final static class MyAgent implements IntegratorBox.Forcible, Serializable {  //need public so to use with instanceof
        private static final long serialVersionUID = 1L;
        public IVector force;

        public MyAgent(ISpace space) {
            force = space.makeVector();
        }
        
        public IVector force() {return force;}
    }
    
}
