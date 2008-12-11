package etomica.simulation.prototypes;
import etomica.action.BoxImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.atom.AtomTypeSphere;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.graphics.DisplayBox;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.potential.P2SquareWell;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space2d.Space2D;
import etomica.species.SpeciesSpheresMono;

/**
 * Simple square-well molecular dynamics simulation in 2D
 */
 
public class SwMd2D extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public IntegratorHard integrator;
    public SpeciesSpheresMono species;
    public IBox box;
    public P2SquareWell potential;
    public Controller controller;
    public DisplayBox display;

    public SwMd2D() {
        super(Space2D.getInstance());
        IPotentialMaster potentialMaster = new PotentialMasterMonatomic(this);
        double sigma = 0.8;
        integrator = new IntegratorHard(this, potentialMaster, space);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setSleepPeriod(1);
        integrator.setTimeStep(0.02);
        integrator.setTemperature(1.);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        AtomTypeSphere leafType = (AtomTypeSphere)species.getLeafType();
        leafType.setDiameter(sigma);
        box = new Box(this, space);
        addBox(box);
        box.setNMolecules(species, 50);
        new ConfigurationLattice(new LatticeOrthorhombicHexagonal(space), space).initializeCoordinates(box);
        potential = new P2SquareWell(space);
        potential.setCoreDiameter(sigma);
        potentialMaster.addPotential(potential,new IAtomTypeLeaf[]{leafType,leafType});
        
        integrator.setBox(box);
        integrator.addIntervalAction(new BoxImposePbc(box, space));
    } 
}