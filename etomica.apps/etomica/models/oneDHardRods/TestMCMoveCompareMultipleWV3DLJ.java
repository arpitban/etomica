package etomica.models.oneDHardRods;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomList;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IVectorMutable;
import etomica.atom.Atom;
import etomica.box.Box;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.NormalModes;
import etomica.normalmode.NormalModesFromFile;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncatedShifted;
import etomica.potential.Potential2SoftSpherical;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformableLattice;
import etomica.space.Space;
import etomica.space3d.Vector3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;

public class TestMCMoveCompareMultipleWV3DLJ extends Simulation {
    private static final long serialVersionUID = 1L;
    public Boundary boundary;
    IntegratorMC integrator;
    ActivityIntegrate activityIntegrate;
    IBox box;
    CoordinateDefinition coordinateDefinition;
    Primitive primitive;
    Basis basis;
    int[] nCells;
    SpeciesSpheresMono species;
    NormalModes nm;
    double[] locations;
    
    MCMoveCompareMultipleWV move; 
    
    private static final String APP_NAME = "TestMCMove";
    

    public TestMCMoveCompareMultipleWV3DLJ(Space _space, int numAtoms, double density, double 
            temperature, String filename, double harmonicFudge, int[] cwv, int[] hwv){
        super(_space);
        
//        long seed = 5;
//        System.out.println("Seed explicitly set to " + seed);
//        IRandom rand = new RandomNumberGenerator(seed);
//        this.setRandom(rand);
        
        //Set up some of the joint stuff
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        addSpecies(species);
        
        basis = new BasisMonatomic(space);
        
        PotentialMasterMonatomic potentialMaster = new 
                PotentialMasterMonatomic(this);
        integrator = new IntegratorMC(this, potentialMaster);
        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, numAtoms);
        integrator.setBox(box);
        
        primitive = new PrimitiveCubic(space, 1.0);
        double v = primitive.unitCell().getVolume();
        primitive.scaleSize(Math.pow(v*density/4, -1.0/3.0));
        int numberOfCells = (int)Math.round(Math.pow(numAtoms/4, 1.0/3.0));
        nCells = new int[]{numberOfCells, numberOfCells, numberOfCells};
        boundary = new BoundaryDeformableLattice(primitive, nCells);
        box.setBoundary(boundary);
        Basis basis = new BasisCubicFcc();
        
        CoordinateDefinitionLeaf coordinateDefinition = new CoordinateDefinitionLeaf(box, primitive, basis, space);
        coordinateDefinition.initializeCoordinates(nCells);
        
        Potential2SoftSpherical p2 = new P2LennardJones(space, 1.0, 1.0);
        double truncationRadius = boundary.getBoxSize().getX(0) * 0.495;
        P2SoftSphericalTruncatedShifted pTruncated = new 
                P2SoftSphericalTruncatedShifted(space, p2, truncationRadius);
        potentialMaster.addPotential(pTruncated, new IAtomType[]
                {species.getLeafType(), species.getLeafType()});
        
        nm = new NormalModesFromFile(filename, space.D());
        
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        nm.getOmegaSquared();
        
        WaveVectorFactory waveVectorFactory = nm.getWaveVectorFactory();
        waveVectorFactory.makeWaveVectors(box);
        int wvflength = waveVectorFactory.getWaveVectors().length;
        System.out.println("We have " + wvflength +" wave vectors.");
        System.out.println("Wave Vector Coefficients:");
        for(int i = 0; i < wvflength; i++){
            System.out.println(i + " " + waveVectorFactory.getCoefficients()[i]);
        }
        
        move = new MCMoveCompareMultipleWV(potentialMaster, random);
        integrator.getMoveManager().addMCMove(move);
        move.setWaveVectors(waveVectorFactory.getWaveVectors());
        move.setWaveVectorCoefficients(waveVectorFactory.getCoefficients());
        move.setEigenVectors(nm.getEigenvectors());
        move.setOmegaSquared(nm.getOmegaSquared(), nm.getWaveVectorFactory().getCoefficients());
        move.setCoordinateDefinition(coordinateDefinition);
        move.setBox((IBox)box);
        move.setStepSizeMin(0.001);
        move.setStepSize(0.01);
        move.setComparedWV(cwv);
        move.setChangeableWVs(hwv);
        
        
        activityIntegrate = new ActivityIntegrate(integrator, 0, true);
        getController().addAction(activityIntegrate);
        
    }

    
    public static void main(String args[]){
        SimOverlapSingleWaveVector3DParam params = new SimOverlapSingleWaveVector3DParam();
        String inputFilename = null;
        if(args.length > 0){
            inputFilename = args[0];
        }
        if(inputFilename != null){
            ReadParameters readParameters = new 
                ReadParameters(inputFilename, params);
            readParameters.readParameters();
        }
        
        int numMolecules = params.numAtoms;
        double density = params.density;
        int D = params.D;
        double harmonicFudge = params.harmonicFudge;
        String filename = params.filename;
        if(filename.length() == 0){
            filename = "1DHR";
        }
        double temperature = params.temperature;
        int[] comparedWV = params.comparedWVs;
        int[] changeableWV = params.changeableWVs;
        
        String refFileName = args.length > 0 ? filename+"_ref" : null;
        
        //instantiate simulations!
        TestMCMoveCompareMultipleWV3DLJ sim = new TestMCMoveCompareMultipleWV3DLJ  (Space.getInstance(D), numMolecules,
                density, temperature, filename, harmonicFudge, comparedWV, changeableWV);
        int numSteps = params.numSteps;
        
        System.out.println("Running Nancy's single " +D+"D Lennard Jones simulation");
        System.out.println(numMolecules+" atoms at density "+density);
        System.out.println("harmonic fudge: "+harmonicFudge);
        System.out.println("temperature: " + temperature);
        System.out.println("compared wave vector: " + comparedWV);
        System.out.println("Total steps: "+params.numSteps);
        System.out.println("instantiated");
        
        //collect initial location data
        IVectorMutable[] locationsOld = new Vector3D[numMolecules];
        IAtomList leaflist = sim.box.getLeafList();
        double oldX = 0.0; double oldY = 0.0; double oldZ = 0.0;
        for(int i = 0; i < numMolecules; i++){
            //one d is assumed here.
            locationsOld[i] = ( ((Atom)leaflist.getAtom(i)).getPosition() );
        }
        for(int i = 0; i < numMolecules; i++){
            oldX += locationsOld[i].getX(0);
            oldY += locationsOld[i].getX(1);
            oldZ += locationsOld[i].getX(2);
        }
        
        sim.activityIntegrate.setMaxSteps(numSteps);
        sim.getController().actionPerformed();
        
        //see if anything moved:
        IVectorMutable[] locationsNew = new Vector3D[numMolecules];
        leaflist = sim.box.getLeafList();
        double newX = 0.0; double newY = 0.0; double newZ = 0.0;
        for(int i = 0; i < numMolecules; i++){
            //one d is assumed here.
            locationsNew[i] = ( ((Atom)leaflist.getAtom(i)).getPosition() );
        }
        for(int i = 0; i < numMolecules; i++){
            newX += locationsOld[i].getX(0);
            newY += locationsOld[i].getX(1);
            newZ += locationsOld[i].getX(2);
        }
        System.out.println("Old locations:  " + oldX + "  " + oldY + "  " + oldZ);
        System.out.println("New locations:  " + newX + "  " + newY + "  " + newZ);

        System.out.println("Fini.");
    }
    
    
    public void setComparedWV(int[] awv){
        move.setComparedWV(awv);
    }
    
    public static class SimOverlapSingleWaveVector3DParam extends ParameterBase {
        public int numAtoms = 32;
        public double density = 0.962;
        public int D = 3;
        public double harmonicFudge = 1.0;
        public double temperature = 0.1378;
        public int[] comparedWVs = { 0, 7};
        public int[] changeableWVs = { 5, 3};
        
        public int numSteps = 1000;
        
        public String filename = "normal_modes_LJ_3D_32";


    }
 
}

