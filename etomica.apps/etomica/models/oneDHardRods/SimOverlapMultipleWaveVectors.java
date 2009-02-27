package etomica.models.oneDHardRods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IRandom;
import etomica.box.Box;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.DataPump;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.math.SpecialFunctions;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.NormalModes1DHR;
import etomica.normalmode.P2XOrder;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.potential.Potential2HardSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.RandomNumberGenerator;
import etomica.util.ReadParameters;
import etomica.virial.overlap.AccumulatorVirialOverlapSingleAverage;
import etomica.virial.overlap.DataSourceVirialOverlap;
import etomica.virial.overlap.IntegratorOverlap;

public class SimOverlapMultipleWaveVectors extends Simulation {
    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "SimOverlapMultipleWaveVectors";
    Primitive primitive;
    int[] nCells;
    NormalModes1DHR nm;
    double bennettParam;       //adjustable parameter - Bennett's parameter
    public IntegratorOverlap integratorSim; //integrator for the whole simulation
    public DataSourceVirialOverlap dsvo;
    public BasisMonatomic basis;
    ActivityIntegrate activityIntegrate;
    
    IntegratorMC[] integrators;
    public AccumulatorVirialOverlapSingleAverage[] accumulators;
    public DataPump[] accumulatorPumps;
    public IEtomicaDataSource[] meters;
    public IBox boxTarget, boxRef;
    public Boundary boundaryTarget, boundaryRef;
    MCMoveChangeMultipleModes changeMove;
    MCMoveCompareMultipleModes compareMove;
    MeterPotentialEnergy meterAinB, meterAinA;
    MeterCompareMultipleModesBrute meterBinA, meterBinB;
    
    
    public SimOverlapMultipleWaveVectors(Space _space, int numAtoms, double 
            density, double temperature, String filename, double harmonicFudge,
            int[] compWV, int[] harmWV){
        super(_space, true);
        
//        IRandom rand = new RandomNumberGenerator((long)3.0);
//        this.setRandom(rand);
        
        //Set up some of the joint stuff
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
//        basis = new BasisMonatomic(space);
        
        integrators = new IntegratorMC[2];
        accumulatorPumps = new DataPump[2];
        meters = new IEtomicaDataSource[2];
        accumulators = new AccumulatorVirialOverlapSingleAverage[2];
        
        basis = new BasisMonatomic(space);
        
        
//        bMeters = new IEtomicaDataSource[2];
//        bPumps = new DataPump[2];
//        bAccs = new AccumulatorVirialOverlapSingleAverage[2];
        
        
        
//TARGET    
        // Set up target system - A, 1, hard rod
        PotentialMasterList potentialMasterTarget = new PotentialMasterList(
                this, space);
        boxTarget = new Box(space);
        addBox(boxTarget);
        boxTarget.setNMolecules(species, numAtoms);
        
        Potential2 p2 = new P2HardSphere(space, 1.0, true);
        p2 = new P2XOrder(space, (Potential2HardSpherical)p2);
        p2.setBox(boxTarget);
        potentialMasterTarget.addPotential(p2, new IAtomType[] {
                species.getLeafType(), species.getLeafType()});
        
        primitive = new PrimitiveCubic(space, 1.0/density);
        boundaryTarget = new BoundaryRectangularPeriodic(space, numAtoms/density);
        nCells = new int[]{numAtoms};
        boxTarget.setBoundary(boundaryTarget);
        
        CoordinateDefinitionLeaf coordinateDefinitionTarget = new 
                CoordinateDefinitionLeaf(this, boxTarget, primitive, space);
        coordinateDefinitionTarget.initializeCoordinates(nCells);
        
        double neighborRange = 1.01/density;
        potentialMasterTarget.setRange(neighborRange);
        // Find neighbors now.  Don't hook up the NieghborListManager since the
        //  neighbors won't change.
        potentialMasterTarget.getNeighborManager(boxTarget).reset();
        
        IntegratorMC integratorTarget = new IntegratorMC(potentialMasterTarget,
                random, temperature);
        integrators[1] = integratorTarget;
        integratorTarget.setBox(boxTarget);
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        
        WaveVectorFactory waveVectorFactoryTarget = nm.getWaveVectorFactory();
        waveVectorFactoryTarget.makeWaveVectors(boxTarget);
        int wvflength = waveVectorFactoryTarget.getWaveVectors().length;
        System.out.println("We have " + wvflength +" wave vectors.");
        System.out.println("Wave Vector Coefficients:");
        for(int i = 0; i < wvflength; i++){
            System.out.println(i + " " + waveVectorFactoryTarget.getCoefficients()[i]);
        }
        
        changeMove = new MCMoveChangeMultipleModes(potentialMasterTarget, random);
        integratorTarget.getMoveManager().addMCMove(changeMove);
        changeMove.setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
        changeMove.setWaveVectorCoefficients(waveVectorFactoryTarget.getCoefficients());
        changeMove.setEigenVectors(nm.getEigenvectors(boxTarget));
        changeMove.setCoordinateDefinition(coordinateDefinitionTarget);
        changeMove.setBox((IBox)boxTarget);
        changeMove.setStepSizeMin(0.001);
        changeMove.setStepSize(0.01);
        
        meterAinA = new MeterPotentialEnergy(potentialMasterTarget);
        meterAinA.setBox(boxTarget);
        
        meterBinA = new MeterCompareMultipleModesBrute("meterBinA", 
                potentialMasterTarget, coordinateDefinitionTarget, boxTarget);
        meterBinA.setEigenVectors(nm.getEigenvectors(boxTarget));
        meterBinA.setOmegaSquared(nm.getOmegaSquared(boxTarget));
        meterBinA.setTemperature(temperature);
        meterBinA.setWaveVectorCoefficients(waveVectorFactoryTarget.getCoefficients());
        meterBinA.setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
        
        MeterOverlap meterOverlapInA = new MeterOverlap("meterOverlapInA", Null.DIMENSION, 
                meterAinA, meterBinA, temperature);
        meters[1] = meterOverlapInA;
        
        potentialMasterTarget.getNeighborManager(boxTarget).reset();
        
        meterBinA.getSingle().setCoordinateDefinition(coordinateDefinitionTarget);
        meterBinA.getSingle().setEigenVectors(nm.getEigenvectors(boxTarget));
        meterBinA.getSingle().setOmegaSquared(nm.getOmegaSquared(boxTarget));
        meterBinA.getSingle().setTemperature(temperature);
        meterBinA.getSingle().setWaveVectorCoefficients(waveVectorFactoryTarget.getCoefficients());
        meterBinA.getSingle().setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
        meterBinA.setA(true);
        
//        singleBinA = new MeterCompareSingleModeBrute("singleBinA", potentialMasterTarget, coordinateDefinitionTarget, boxTarget);
//        singleBinA.setEigenVectors(nm.getEigenvectors(boxTarget));
//        singleBinA.setOmegaSquared(nm.getOmegaSquared(boxTarget));
//        singleBinA.setTemperature(temperature);
//        singleBinA.setWaveVectorCoefficients(waveVectorFactoryTarget.getCoefficients());
//        singleBinA.setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
//        singleBinA.setComparedWV(11);
//        System.out.println("singleBinA has set its comparedWV to 11");
//        
//        MeterOverlap singleOverlapinA = new MeterOverlap("singleOverlapinA", Null.DIMENSION, meterAinA, singleBinA, temperature);
//        bMeters[1] = singleOverlapinA;
        
        
        
        
        
        
//REFERENCE
        // Set up REFERENCE system - System B - 0 - Hybrid system
        PotentialMasterList potentialMasterRef = new PotentialMasterList(this, space);
        boxRef = new Box(space);
        addBox(boxRef);
        boxRef.setNMolecules(species, numAtoms);
        
        p2 = new P2HardSphere(space, 1.0, true);
        p2 = new P2XOrder(space, (Potential2HardSpherical)p2);
        p2.setBox(boxRef);
        potentialMasterRef.addPotential(p2, new IAtomType[] {
                species.getLeafType(), species.getLeafType()});
        
        primitive = new PrimitiveCubic(space, 1.0/density);
        boundaryRef = new BoundaryRectangularPeriodic(space, numAtoms/density);
        nCells = new int[]{numAtoms};
        boxRef.setBoundary(boundaryRef);
        
        CoordinateDefinitionLeaf coordinateDefinitionRef = new 
                CoordinateDefinitionLeaf(this, boxRef, primitive, space);
        coordinateDefinitionRef.initializeCoordinates(nCells);
        
        neighborRange = 1.01/density;
        potentialMasterRef.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMasterRef.getNeighborManager(boxRef).reset();
        
        IntegratorMC integratorRef = new IntegratorMC(potentialMasterRef, 
                random, temperature);
        integratorRef.setBox(boxRef);
        integrators[0] = integratorRef;
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        
        WaveVectorFactory waveVectorFactoryRef = nm.getWaveVectorFactory();
        waveVectorFactoryRef.makeWaveVectors(boxRef);
        
        compareMove = new MCMoveCompareMultipleModes(potentialMasterRef, 
                random);
        integratorRef.getMoveManager().addMCMove(compareMove);
        compareMove.setWaveVectors(waveVectorFactoryRef.getWaveVectors());
        compareMove.setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
        compareMove.setOmegaSquared(nm.getOmegaSquared(boxRef), 
                waveVectorFactoryRef.getCoefficients());
        compareMove.setEigenVectors(nm.getEigenvectors(boxRef));
        compareMove.setCoordinateDefinition(coordinateDefinitionRef);
        compareMove.setTemperature(temperature);
        compareMove.setBox((IBox)boxRef);
        compareMove.setStepSizeMin(0.001);
        compareMove.setStepSize(0.01);
        
        meterAinB = new MeterPotentialEnergy(potentialMasterRef);
        meterAinB.setBox(boxRef);
       
        meterBinB = new MeterCompareMultipleModesBrute(potentialMasterRef,
                coordinateDefinitionRef, boxRef);
        meterBinB.setCoordinateDefinition(coordinateDefinitionRef);
        meterBinB.setEigenVectors(nm.getEigenvectors(boxRef));
        meterBinB.setOmegaSquared(nm.getOmegaSquared(boxRef));
        meterBinB.setTemperature(temperature);
        meterBinB.setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
        meterBinB.setWaveVectors(waveVectorFactoryRef.getWaveVectors());
        integratorRef.setMeterPotentialEnergy(meterBinB);
        
        MeterOverlap meterOverlapInB = new MeterOverlap("MeterOverlapInB", Null.DIMENSION, 
                meterBinB, meterAinB, temperature);
        meters[0] = meterOverlapInB;
        
        integratorRef.setBox(boxRef);
        potentialMasterRef.getNeighborManager(boxRef).reset();
        
        
        meterBinB.getSingle().setCoordinateDefinition(coordinateDefinitionRef);
        meterBinB.getSingle().setEigenVectors(nm.getEigenvectors(boxRef));
        meterBinB.getSingle().setOmegaSquared(nm.getOmegaSquared(boxRef));
        meterBinB.getSingle().setTemperature(temperature);
        meterBinB.getSingle().setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
        meterBinB.getSingle().setWaveVectors(waveVectorFactoryRef.getWaveVectors());
        meterBinB.setA(false);
        
//        singleBinB = new MeterCompareSingleModeBrute(potentialMasterRef, 
//                coordinateDefinitionRef, boxRef);
//        singleBinB.setCoordinateDefinition(coordinateDefinitionRef);
//        singleBinB.setEigenVectors(nm.getEigenvectors(boxRef));
//        singleBinB.setOmegaSquared(nm.getOmegaSquared(boxRef));
//        singleBinB.setTemperature(temperature);
//        singleBinB.setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
//        singleBinB.setWaveVectors(waveVectorFactoryRef.getWaveVectors());
//        
//        
//        MeterOverlap singleOverlapInB = new MeterOverlap("SingleOverlapInB", Null.DIMENSION, singleBinB, meterAinB, temperature);
//        bMeters[0] = singleOverlapInB;
        
        
        
//JOINT
        //Set up the rest of the joint stuff
        setComparedWV(compWV);
        setHarmonicWV(harmWV);
        
        integratorSim = new IntegratorOverlap(random, new 
                IntegratorMC[]{integratorRef, integratorTarget});
        
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, true), 0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, false), 1);
        
        setBennettParameter(1.0, 30);
        
        activityIntegrate = new ActivityIntegrate(integratorSim, 0, true);
        getController().addAction(activityIntegrate);
        
    }
    
    public void setBennettParameter(double benParamCenter, double span) {
        bennettParam = benParamCenter;
        accumulators[0].setBennetParam(benParamCenter,span);
        accumulators[1].setBennetParam(benParamCenter,span);
    }
    
    public void setBennettParameter(double newBennettParameter) {
        System.out.println("setting ref pref (explicitly) to "+
                newBennettParameter);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
        setBennettParameter(newBennettParameter,1);
        
    }
    
    public void initBennettParameter(String fileName, int initSteps, int initBlockSize) {
        // benParam = -1 indicates we are searching for an appropriate value
        bennettParam = -1.0;
        integratorSim.getMoveManager().setEquilibrating(true);
        
        if (fileName != null) {
            try { 
                FileReader fileReader = new FileReader(fileName);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String benParamString = bufReader.readLine();
                bennettParam = Double.parseDouble(benParamString);
                bufReader.close();
                fileReader.close();
                System.out.println("setting ref pref (from file) to "+bennettParam);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
                setBennettParameter(bennettParam,1);
            }
            catch (IOException e) {
                System.out.println("Bennett parameter not from file");
                // file not there, which is ok.
            }
        }
        
        if (bennettParam == -1) {
            
//            int oldBlockSize = blockSize;
//            long newBlockSize = initSteps*integratorSim.getNumSubSteps()/1000;
//            //Make sure the new block size is reasonable.
//            if(newBlockSize < 1000){
//                newBlockSize = 1000;
//            }
//            if(newBlockSize > 1000000){
//                newBlockSize = 1000000;
//            }
//            setAccumulatorBlockSize((int)newBlockSize);
            
            // equilibrate off the lattice to avoid anomolous contributions
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,41,false),1);
            setBennettParameter(1e40,40);
            activityIntegrate.setMaxSteps(initSteps);
            
            getController().actionPerformed();
            getController().reset();

            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            
            double top = accumulators[0].getBennetAverage(newMinDiffLoc);
            System.out.println("top " + top);
            double bottom = accumulators[1].getBennetAverage(newMinDiffLoc);
            System.out.println("bottom " + bottom);
            
            if (Double.isNaN(bennettParam) || bennettParam == 0 || Double.isInfinite(bennettParam)) {
                throw new RuntimeException("Simulation failed to find a valid ref pref");
            }
            System.out.println("setting ref pref to "+bennettParam);
//            setAccumulatorBlockSize(oldBlockSize);
            
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,false),1);
            setBennettParameter(bennettParam,5);
            
            // set benParam back to -1 so that later on we know that we've been looking for
            // the appropriate value
            bennettParam = -1;
            getController().reset();
        }
        integratorSim.getMoveManager().setEquilibrating(false);
    }
    
    public void setAccumulator(AccumulatorVirialOverlapSingleAverage 
            newAccumulator, int iBox) {
        accumulators[iBox] = newAccumulator;
        if (accumulatorPumps[iBox] == null) {
            accumulatorPumps[iBox] = new DataPump(meters[iBox], newAccumulator);
            integrators[iBox].addIntervalAction(accumulatorPumps[iBox]);
//            integrators[iBox].setActionInterval(accumulatorPumps[iBox], 
//                    boxRef.getLeafList().getAtomCount()*2);
            integrators[iBox].setActionInterval(accumulatorPumps[iBox], 1);
        }
        else {
            accumulatorPumps[iBox].setDataSink(newAccumulator);
        }
        if (integratorSim != null && accumulators[0] != null && 
                accumulators[1] != null) {
            dsvo = new DataSourceVirialOverlap(accumulators[0],accumulators[1]);
            integratorSim.setDSVO(dsvo);
        }
        
    }
    
    public void setAccumulatorBlockSize(int newBlockSize) {
        for (int i=0; i<2; i++) {
            accumulators[i].setBlockSize(newBlockSize);
        }
        try {
            // reset the integrator so that it will re-adjust step frequency
            // and ensure it will take enough data for both ref and target
            integratorSim.reset();
        }
        catch (ConfigurationOverlapException e) { /* meaningless */ }
    }
    public void equilibrate(String fileName, int initSteps, int initBlockSize) {
        // run a short simulation to get reasonable MC Move step sizes and
        // (if needed) narrow in on a reference preference
        activityIntegrate.setMaxSteps(initSteps);
        
        integratorSim.getMoveManager().setEquilibrating(true);
        
        //This code allows the computer to set the block size for the main
        //simulation and equilibration/finding alpha separately.
//        int oldBlockSize = blockSize;
//        long newBlockSize = initSteps*integratorSim.getNumSubSteps()/1000;
//        //make sure new block size is reasonablel
//        if(newBlockSize < 1000){
//            newBlockSize = 1000;
//        }
//        if (newBlockSize >1000000) {
//            newBlockSize = 1000000;
//        }
//        setAccumulatorBlockSize((int)newBlockSize);
        
//        setAccumulatorBlockSize((int)eqBlockSize);
        
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(true);
        }
        getController().actionPerformed();
        getController().reset();
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(false);
        }
        
        if (bennettParam == -1) {
            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            System.out.println("setting ref pref to "+bennettParam+" ("+newMinDiffLoc+")");
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(initBlockSize,1,false),1);
            setBennettParameter(bennettParam,1);
            if (fileName != null) {
                try {
                    FileWriter fileWriter = new FileWriter(fileName);
                    BufferedWriter bufWriter = new BufferedWriter(fileWriter);
                    bufWriter.write(String.valueOf(bennettParam)+"\n");
                    bufWriter.close();
                    fileWriter.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("couldn't write to Bennet parameter file");
                }
            }
        }
        else {
            dsvo.reset();
        }
        
        integratorSim.getMoveManager().setEquilibrating(false);
//        setAccumulatorBlockSize(oldBlockSize);
    }
    
    public static void main(String args[]){
        SimOverlapMultipleWaveVectorsParam params = new SimOverlapMultipleWaveVectorsParam();
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
        int[] comparedWV = params.comparedWV;
        int[] harmonicWV = params.harmonicWV;
        if(harmonicWV[0]== -1){
            int[] empty = { };
            harmonicWV = empty;
        }
        
        int numSteps = params.numSteps;
        int runBlockSize = params.runBlockSize;
        int subBlockSize = params.subBlockSize;
        
        int numEqSteps = params.eqNumSteps;
        int eqBlockSize = params.eqBlockSize;
    
        int numBenSteps = params.bennettNumSteps;
        int benBlockSize = params.benBlockSize;
        
        String refFileName = args.length > 0 ? filename+"_ref" : null;
        
        System.out.println("Running Nancy's 1DHR simulation");
        System.out.println(numMolecules+" atoms at density "+density);
        System.out.println("harmonic fudge: "+harmonicFudge);
        System.out.println("temperature: " + temperature);
        System.out.println("compared wave vectors: ");
        for(int i = 0; i < comparedWV.length; i++){
            System.out.println(comparedWV[i]);
        }
        System.out.println("harmonic wave vectors: ");
        for(int i = 0; i < harmonicWV.length; i++ ){
            System.out.println(harmonicWV[i]);
        }
        System.out.println("Total steps: "+numSteps+" , split into blocks of "+runBlockSize);
        System.out.println(subBlockSize+" steps in subintegrator, per step in  main integrator");
        System.out.println(numEqSteps+" equilibration steps, split into blocks of "+ eqBlockSize);
        System.out.println(numBenSteps+" Bennett-only steps, split into blocks of "+benBlockSize);
        System.out.println("output data to "+filename);
        
        //instantiate simulations!
        SimOverlapMultipleWaveVectors sim = new SimOverlapMultipleWaveVectors(Space.getInstance(D), numMolecules,
                density, temperature, filename, harmonicFudge, comparedWV, harmonicWV);
        System.out.println("instantiated");
        
        //Divide out all the steps, so that the subpieces have the proper # of steps
        numSteps /= (int)subBlockSize;
        numEqSteps /= (int)subBlockSize;
        numBenSteps /= subBlockSize;
        
        //start simulation & equilibrate
        sim.integratorSim.getMoveManager().setEquilibrating(true);
        sim.integratorSim.setNumSubSteps(subBlockSize);
        
        System.out.println("Init Bennett");
        sim.setAccumulatorBlockSize(benBlockSize);
        sim.initBennettParameter(filename, numBenSteps, benBlockSize);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                Double.isInfinite(sim.bennettParam)){
            throw new RuntimeException("Simulation failed to find a valid " +
                    "Bennett parameter");
        }
        
        System.out.println("equilibrate");
        sim.setAccumulatorBlockSize(eqBlockSize);
        sim.equilibrate(refFileName, numEqSteps, eqBlockSize);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || 
                Double.isInfinite(sim.bennettParam)){
            throw new RuntimeException("Simulation failed to find a valid " +
                    "Bennett parameter");
        }
        System.out.println("equilibration finished.");
        sim.setAccumulatorBlockSize((int)runBlockSize);
        
        sim.integratorSim.getMoveManager().setEquilibrating(false);
        sim.activityIntegrate.setMaxSteps(numSteps);
        sim.getController().actionPerformed();
        System.out.println("final reference optimal step frequency " + 
                sim.integratorSim.getStepFreq0() + " (actual: " + 
                sim.integratorSim.getActualStepFreq0() + ")");
        
        double[][] omega2 = sim.nm.getOmegaSquared(sim.boxTarget); 
        //Above known from the analytical results. - otherwise it would be from 
        //the S matrix.
        double[] coeffs = sim.nm.getWaveVectorFactory().getCoefficients();
        
        //CALCULATION OF HARMONIC ENERGY
        double AHarmonic = 0;
        for(int i=0; i<omega2.length; i++) {
            for(int j=0; j<omega2[0].length; j++) {
                if (!Double.isInfinite(omega2[i][j])) {
                    AHarmonic += coeffs[i] * Math.log(omega2[i][j]*coeffs[i] /
                            (temperature*Math.PI));
                }
            }
        }
        int totalCells = 1;
        for (int i=0; i<D; i++) {
            totalCells *= sim.nCells[i];
        }
        int basisSize = sim.basis.getScaledCoordinates().length;
        double fac = 1;
        if (totalCells % 2 == 0) {
            fac = Math.pow(2,D);
        }
        AHarmonic -= Math.log(Math.pow(2.0, basisSize *D * (totalCells - fac) / 
                2.0) / Math.pow(totalCells, 0.5 * D));
        System.out.println("Harmonic-reference free energy: " + AHarmonic * 
                temperature);
        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("free energy difference: " + (-Math.log(ratio)) + 
                ", error: "+(error/ratio));
        System.out.println("target free energy: " + (AHarmonic-Math.log(ratio)));
        DataGroup allYourBase = 
            (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("harmonic ratio average: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                 + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints() -
                sim.dsvo.minDiffLocation()-1);
        System.out.println("target ratio average: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                 + " error: " + 
                ((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
    
        if(D==1) {
            double AHR = -(numMolecules-1)*Math.log(numMolecules/density-numMolecules)
                + SpecialFunctions.lnFactorial(numMolecules) ;
            System.out.println("Hard-rod free energy: "+AHR);
        }
    }
    
    public void setComparedWV(int[] cwvs){
        meterBinB.setComparedWVs(cwvs);
        meterBinA.setComparedWVs(cwvs);
        compareMove.setComparedWV(cwvs);
        
        meterBinB.getSingle().setComparedWV(cwvs[0]);
        meterBinA.getSingle().setComparedWV(cwvs[0]);
        
        
    }
    
    public void setHarmonicWV(int[] hwv){
        compareMove.setHarmonicWV(hwv);
        changeMove.setHarmonicWV(hwv);
    }
    
    public static class SimOverlapMultipleWaveVectorsParam extends ParameterBase {
        public int numAtoms = 10;
        public double density = 0.5;
        public int D = 1;
        public double harmonicFudge = 1.0;
        public String filename = "HR1D_";
        public double temperature = 1.0;
        public int[] comparedWV = {4, 1};
        public int[] harmonicWV = {2, 5};
        
        public int numSteps = 400000;
        public int runBlockSize = 1000;
        public int subBlockSize = 10;    //# of steps in subintegrator per integrator step
        
        public int eqNumSteps = 40000;  
        public int eqBlockSize = 100;
        
        public int bennettNumSteps = 40000;
        public int benBlockSize = 100;
    }
}
