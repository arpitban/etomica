package etomica.threaded.atom;

import etomica.atom.AtomPositionDefinition;
import etomica.atom.AtomSet;
import etomica.atom.IAtom;
import etomica.atom.IAtomGroup;
import etomica.atom.iterator.IteratorDirective;
import etomica.nbr.PotentialGroupNbr;
import etomica.nbr.cell.BoxAgentSourceCellManager;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.box.Box;
import etomica.box.BoxAgentManager;
import etomica.potential.IPotential;
import etomica.potential.PotentialArray;
import etomica.potential.PotentialCalculation;
import etomica.simulation.ISimulation;
import etomica.threaded.IPotentialCalculationThreaded;
import etomica.util.Debug;

public class PotentialMasterListThreaded extends PotentialMasterList {

    private static final long serialVersionUID = 1L;
    PotentialMasterListWorker[] threads;
	int ready1;
	int ready2;
	
	
	public PotentialMasterListThreaded(ISimulation sim) {
		super(sim);
		// TODO Auto-generated constructor stub
	}

	public PotentialMasterListThreaded(ISimulation sim, double range) {
		super(sim, range);
		// TODO Auto-generated constructor stub
	}

	public PotentialMasterListThreaded(ISimulation sim, double range,
			AtomPositionDefinition positionDefinition) {
		super(sim, range, positionDefinition);
		// TODO Auto-generated constructor stub
	}

	public PotentialMasterListThreaded(ISimulation sim, double range,
			BoxAgentSourceCellManager boxAgentSource) {
		super(sim, range, boxAgentSource);
		// TODO Auto-generated constructor stub
	}

	public PotentialMasterListThreaded(ISimulation sim, double range,
			BoxAgentSourceCellManager boxAgentSource,
			BoxAgentManager agentManager) {
		super(sim, range, boxAgentSource, agentManager);
		// TODO Auto-generated constructor stub
	}
	
    public void calculate(Box box, IteratorDirective id, PotentialCalculation pc) {
        if(!enabled) return;
        IAtom targetAtom = id.getTargetAtom();
        NeighborListManager neighborManager = (NeighborListManager)neighborListAgentManager.getAgent(box);

        if (targetAtom == null) {
            //no target atoms specified -- do one-target algorithm to SpeciesMaster
            if (Debug.ON && id.direction() != IteratorDirective.Direction.UP) {
                throw new IllegalArgumentException("When there is no target, iterator directive must be up");
            }
            // invoke setBox on all potentials
            for (int i=0; i<allPotentials.length; i++) {
                allPotentials[i].setBox(box);
            }
            
            if(pc instanceof IPotentialCalculationThreaded){
            	calculateThreaded(box, id, (IPotentialCalculationThreaded)pc, neighborManager);
            }
            else{
            	//method of super class
            	super.calculate(box, id, pc);
            }
            
        }
        else {
            //first walk up the tree looking for 1-body range-independent potentials that apply to parents
            IAtom parentAtom = targetAtom.getParentGroup();
            while (parentAtom.getType().getDepth() > 1) {
                PotentialArray potentialArray = getIntraPotentials(parentAtom.getType());
                IPotential[] potentials = potentialArray.getPotentials();
                for(int i=0; i<potentials.length; i++) {
                    potentials[i].setBox(box);
                    ((PotentialGroupNbr)potentials[i]).calculateRangeIndependent(parentAtom,id,pc);
                }
                parentAtom = parentAtom.getParentGroup();
            }                
            PotentialArray potentialArray = (PotentialArray)rangedAgentManager.getAgent(targetAtom.getType());
            IPotential[] potentials = potentialArray.getPotentials();
            for(int i=0; i<potentials.length; i++) {
                potentials[i].setBox(box);
            }
            calculate(targetAtom, id, pc, neighborManager);
        }
       
        if(lrcMaster != null) {
            lrcMaster.calculate(box, id, pc);
        }
    }

    protected void calculateThreaded(Box box, IteratorDirective id, IPotentialCalculationThreaded pc, NeighborListManager neighborManager) {

        //cannot use AtomIterator field because of recursive call
        AtomSet list = box.getSpeciesMaster().getAgentList();
        int size = list.getAtomCount();
        for (int i=0; i<size; i++) {
            IAtom a = list.getAtom(i);
            calculateThreaded(a, id, pc, neighborManager);//recursive call
        }
        pc.writeData();
    }
        
    protected void calculateThreaded(IAtom atom, IteratorDirective id, IPotentialCalculationThreaded pc, NeighborListManager neighborManager) {
           
        AtomSet list = ((IAtomGroup)atom).getChildList();
        int size = list.getAtomCount();
        
    	
			                            
            for(int i=0; i<threads.length; i++){
                synchronized(threads[i]){
                threads[i].startAtom = (i*size)/threads.length;
                threads[i].stopAtom = ((i+1)*size)/threads.length;
                threads[i].id = id;
                threads[i].pc = pc.getPotentialCalculations()[i];
                threads[i].greenLight = true;
                threads[i].finished = false;
                threads[i].notifyAll();
               
                }
            }
	
            
			// All threads are running
            
		
		
        // Waiting for all threads to complete, threads report "Finished!"
		for(int i=0; i<threads.length; i++){
			synchronized(threads[i]){
				try{
                   
					if (!threads[i].finished){
                      
                        threads[i].wait();	
					}
				}
				catch(InterruptedException e){
				}
		
			}
		}
		
        
    }
	
	public void setNumThreads(int t, Box box){
		threads = new PotentialMasterListWorker[t];
				
        for (int i=0; i<t; i++){
			threads[i] = new PotentialMasterListWorker(i, rangedAgentManager, this);
			threads[i].fillNeighborListArray(i, t, (NeighborListManager)neighborListAgentManager.getAgent(box), box);
            threads[i].start();
		}
           
	}
}
