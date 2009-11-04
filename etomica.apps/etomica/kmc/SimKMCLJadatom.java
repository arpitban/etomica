package etomica.kmc;

import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVectorMutable;
import etomica.atom.MoleculeArrayList;
import etomica.box.Box;
import etomica.chem.elements.ElementSimple;
import etomica.config.Configuration;
import etomica.config.ConfigurationFile;
import etomica.config.ConfigurationLattice;
import etomica.dimer.IntegratorDimerRT;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.SimulationGraphic;
import etomica.lattice.LatticeCubicFcc;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P2LennardJones;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom on a surface, modeled with LJ.
 * 
 * @author msellers
 *
 */

public class SimKMCLJadatom extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerLJadatom";
    public final PotentialMaster potentialMaster;
    public IntegratorKMC integratorKMC;
    public IntegratorKMCCluster integratorKMCCluster;
    public IntegratorDimerRT integratorDimer;
    public IBox box;
    public SpeciesSpheresMono fixed, movable;
    public ActivityIntegrate activityIntegrateKMC, activityIntegrateKMCCluster, activityIntegrateDimer;
    public IMoleculeList movableSet;
    public IVectorMutable adAtomPos;
    

    public SimKMCLJadatom() {
    	super(Space3D.getInstance());
    	potentialMaster = new PotentialMasterMonatomic(this);
    	
    //SIMULATION BOX
        box = new Box(new BoundaryRectangularSlit(0, 5, space), space);
        addBox(box);
        
    //SPECIES
    	double sigma = 1.0;
    	fixed = new SpeciesSpheresMono(space, new ElementSimple("A", Double.POSITIVE_INFINITY));
        movable = new SpeciesSpheresMono(this, space);  
        addSpecies(fixed);
        addSpecies(movable);
        ((IAtomTypeSphere)fixed.getLeafType()).setDiameter(sigma);
        ((IAtomTypeSphere)movable.getLeafType()).setDiameter(sigma);
    	
        // Must be in same order as the respective species is added to SpeciesManager
        box.setNMolecules(fixed, 256);    	
    	
        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(1);
        inflater.actionPerformed();
    	
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomType[]{movable.getLeafType(), fixed.getLeafType()});
		potentialMaster.addPotential(new P2LennardJones(space, sigma, 1.0), new IAtomType[]{movable.getLeafType(), movable.getLeafType()});

		
    //CRYSTAL
        Configuration config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box); 
       
        //ADATOM CREATION AND PLACEMENT
        
        IMolecule iMolecule = movable.makeMolecule();
        box.addMolecule(iMolecule);
        adAtomPos = iMolecule.getChildList().getAtom(0).getPosition();
        //adAtomPos = getSpace().makeVector();
        adAtomPos.setX(0, 3.5);
        adAtomPos.setX(1, -0.30);
        adAtomPos.setX(2, -0.30);
        IVectorMutable newBoxLength = space.makeVector();
        newBoxLength.E(box.getBoundary().getBoxSize());
        newBoxLength.setX(0, 2.0*adAtomPos.getX(0)+2.0);
        box.getBoundary().setBoxSize(newBoxLength);

    }
    
    public void setMovableAtoms(double distance, IVectorMutable center){
        //distance = distance*distance;
        IVectorMutable rij = space.makeVector();
        MoleculeArrayList movableList = new MoleculeArrayList();
        IMoleculeList loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,loopSet.getMolecule(i).getChildList().getAtom(0).getPosition());
            if(rij.getX(0) > (box.getBoundary().getBoxSize().getX(0) - 3.0)){continue;}
            //box.getBoundary().nearestImage(rij);
            if(rij.getX(0)< distance){
               movableList.add(loopSet.getMolecule(i));
            } 
        }
        for (int i=0; i<movableList.getMoleculeCount(); i++){
            IMolecule newMolecule = movable.makeMolecule();
            box.addMolecule(newMolecule);
            newMolecule.getChildList().getAtom(0).getPosition().E(movableList.getMolecule(i).getChildList().getAtom(0).getPosition());
            box.removeMolecule(movableList.getMolecule(i));
        }
        movableSet = box.getMoleculeList(movable);
    }

    
    //Must be run after setMovableAtoms
    public void removeAtoms(double distance, IVectorMutable center){
        distance = distance*distance;
        IVectorMutable rij = space.makeVector();
        
        IMoleculeList loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,loopSet.getMolecule(i).getChildList().getAtom(0).getPosition());
            if(rij.getX(0) > (box.getBoundary().getBoxSize().getX(0) - 3.0)){continue;}
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule(loopSet.getMolecule(i));
            } 
        }   
    }
    
    public void randomizePositions(){
        IVectorMutable workVector = space.makeVector();
        IMoleculeList loopSet3 = box.getMoleculeList(movable);
        IVectorMutable [] currentPos = new IVectorMutable [loopSet3.getMoleculeCount()];
        double offset = 0;
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i] = (loopSet3.getMolecule(i).getChildList().getAtom(0).getPosition());
            for(int j=0; j<3; j++){
                offset = random.nextGaussian()/10.0;
                if(Math.abs(offset)>0.1){offset=0.1;}
                workVector.setX(j,offset);
            }
            currentPos[i].PE(workVector);
        }
    }
    
    public void initializeConfiguration(String fileName){
        ConfigurationFile config = new ConfigurationFile(fileName);
        config.initializeCoordinates(box);
    }
    
    public void integratorKMC(){
        integratorKMC = new IntegratorKMC(this, potentialMaster, 0.7, this.getRandom(), new ISpecies[]{movable}, this.getSpace());
        integratorKMC.setBox(box);
        activityIntegrateKMC = new ActivityIntegrate(integratorKMC);
        getController().addAction(activityIntegrateKMC);
    }
    
    public void integratorKMCCluster(double temp, int steps, int totalSearch){
        integratorKMCCluster = new IntegratorKMCCluster(this, potentialMaster, temp, totalSearch, this.getRandom(), new ISpecies[]{movable}, this.getSpace());
        integratorKMCCluster.setBox(box);
        activityIntegrateKMCCluster = new ActivityIntegrate(integratorKMCCluster);
        activityIntegrateKMCCluster.setMaxSteps(steps);
        getController().addAction(activityIntegrateKMCCluster);
    }
    
public void enableDimerSearch(String fileName, long maxSteps){
        
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new ISpecies[]{movable}, space);
        integratorDimer.setBox(box);
        integratorDimer.setOrtho(false, false);
        integratorDimer.setFileName(fileName);
 
        //integratorDimer.addNonintervalListener(potentialMaster.getNeighborManager(box));
        //integratorDimer.addIntervalAction(potentialMaster.getNeighborManager(box));  
        activityIntegrateDimer = new ActivityIntegrate(integratorDimer);
        integratorDimer.setActivityIntegrate(activityIntegrateDimer);
        getController().addAction(activityIntegrateDimer);
        activityIntegrateDimer.setMaxSteps(maxSteps);
    }

    public static void main(String[] args){
       
        final SimKMCLJadatom sim = new SimKMCLJadatom();
        IVectorMutable vect = sim.getSpace().makeVector();
        vect.setX(0, 3.5);
        vect.setX(1, 0.0);
        vect.setX(2, 0.0);
        
        
        sim.setMovableAtoms(2.0, vect);
        sim.initializeConfiguration("0");
        sim.integratorKMC();
        sim.integratorKMC.createIntegrators();
        sim.integratorKMC.setInitialStateConditions(-539.543484823175, 3.1145942027562522E72);
        sim.integratorKMC.setSearchLimit(1);
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME,1, sim.getSpace(), sim.getController());
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        
        sim.integratorKMC.getEventManager().addListener(new IntegratorListenerAction(simGraphic.getPaintAction(sim.box)));
        sim.integratorKMC.integratorDimer.getEventManager().addListener(new IntegratorListenerAction(simGraphic.getPaintAction(sim.box)));
        sim.integratorKMC.integratorMin1.getEventManager().addListener(new IntegratorListenerAction(simGraphic.getPaintAction(sim.box)));
        sim.integratorKMC.integratorMin2.getEventManager().addListener(new IntegratorListenerAction(simGraphic.getPaintAction(sim.box)));
        
        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        
        colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.red);

        simGraphic.makeAndDisplayFrame(APP_NAME);
    }

}
