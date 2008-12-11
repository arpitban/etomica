package etomica.virial.simulations;

import etomica.api.IAction;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.atom.iterator.ApiBuilder;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.graphics.SimulationGraphic;
import etomica.potential.P2Exp6Buckingham;
import etomica.potential.PotentialGroup;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.Kelvin;
import etomica.util.ParameterBase;
import etomica.virial.ClusterAbstract;
import etomica.virial.MayerEGeneral;
import etomica.virial.MayerEHardSphere;
import etomica.virial.MayerFunction;
import etomica.virial.MayerGeneral;
import etomica.virial.MayerHardSphere;
import etomica.virial.SpeciesAlkane;
import etomica.virial.SpeciesFactory;
import etomica.virial.SpeciesFactorySiepmannSpheres;
import etomica.virial.cluster.Standard;

/**
 * Mayer sampling simulation for alkanes using the dispersion interaction.
 *   Philip J.Lenart and A.Z.Panagiotopoulos, "Tracing the Critical Loci of Binary Fluid
 *   Mixtures Using Molecular Simulation," American Chemical Society(2006)
 */
public class VirialAlkaneMix {


    public static void main(String[] args) {
        VirialAlkaneMixParam params = new VirialAlkaneMixParam();
        final int nPoints;
        int nSpheres1;
        int nSpheres2;
        double temperature;
        long steps;
        int nMethane;
        int nEthane;
        
        
        if (args.length == 0) {
        	nPoints = params.nPoints;
            nSpheres1 = params.nSpheres1;
            nSpheres2 = params.nSpheres2;
            temperature = params.temperature;
            steps = params.numSteps;
            nMethane = params.nMethane;
            nEthane = params.nEthane;
        	
        } else if (args.length == 7) {
        	nPoints = Integer.parseInt(args[0]);
            nSpheres1 = Integer.parseInt(args[1]);
            nSpheres2 = Integer.parseInt(args[2]);
            temperature = Double.parseDouble(args[3]);
            steps = Integer.parseInt(args[4]);
            nMethane = Integer.parseInt(args[5]);
            nEthane = Integer.parseInt(args[6]);
        } else {
        	throw new IllegalArgumentException("Wrong number of arguments");
        }
        
        double sigmaCH4 = 3.741;
        double sigmaCH3 = 3.679;
        double bondL = 1.839;
        double sigmaHSRef = 0.7*(sigmaCH4+sigmaCH3);
        final double[] HSB = new double[8];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);
        HSB[7] = Standard.B7HS(sigmaHSRef);
        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B2HS: "+HSB[2]);
        System.out.println("B3HS: "+HSB[3]+" = "+(HSB[3]/(HSB[2]*HSB[2]))+" B2HS^2");
        System.out.println("B4HS: "+HSB[4]+" = "+(HSB[4]/(HSB[2]*HSB[2]*HSB[2]))+" B2HS^3");
        System.out.println("B5HS: "+HSB[5]+" = 0.110252 B2HS^4");
        System.out.println("B6HS: "+HSB[6]+" = 0.03881 B2HS^5");
        System.out.println("B7HS: "+HSB[7]+" = 0.013046 B2HS^6");
       
		
        Space space = Space3D.getInstance();
        
        MayerHardSphere fRef = new MayerHardSphere(space,sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(space,sigmaHSRef);
        PotentialGroup pMethaneMethaneGroup = new PotentialGroup(2);
        PotentialGroup pMethaneEthaneGroup = new PotentialGroup(2);
        PotentialGroup pEthaneEthaneGroup = new PotentialGroup(2);
        
        System.out.println("Siepman "+nSpheres1+ nSpheres2+nPoints+" at "+temperature+"K");
        temperature = Kelvin.UNIT.toSim(temperature);
        double epsilonCH4 = Kelvin.UNIT.toSim(160.3);
        double epsilonCH3 = Kelvin.UNIT.toSim(129.6);
        double epsilonCH4CH3 = Math.sqrt(epsilonCH4*epsilonCH3);
        double alphaCH4 = 15;
        double alphaCH3 = 16;
        double alphaCH4CH3 = Math.sqrt(alphaCH4*alphaCH3);
        double rmCH4 = 4.183766451;
        double rmCH3 = 4.094113682;
        double rmCH4CH3 = 0.5*(rmCH4 + rmCH3);
        double rmaxCH4 = 0.704;
        double rmaxCH3 = 0.575;
        double rmaxCH4CH3 = 0.5*(rmaxCH4 + rmaxCH3);
        P2Exp6Buckingham p2CH4 = new P2Exp6Buckingham(space, epsilonCH4, alphaCH4, rmCH4, rmaxCH4);
       // for (int i=5; i<100;i++){
        //	System.out.println(i/10.0+" "+p2CH4.u(i*i/100.0));
        	//System.out.println(rmCH4+" "+p2CH4.u(rmCH4*rmCH4));
        //}
        //System.exit(1);
        P2Exp6Buckingham p2CH3 = new P2Exp6Buckingham(space, epsilonCH3, alphaCH3, rmCH3, rmaxCH3);
        P2Exp6Buckingham p2CH4CH3 = new P2Exp6Buckingham(space, epsilonCH4CH3, alphaCH4CH3, rmCH4CH3, rmaxCH4CH3);
        
        MayerGeneral fMethaneMethaneTarget = new MayerGeneral(pMethaneMethaneGroup);
        MayerGeneral fMethaneEthaneTarget = new MayerGeneral(pMethaneEthaneGroup);
        MayerGeneral fEthaneEthaneTarget = new MayerGeneral(pEthaneEthaneGroup);
        MayerEGeneral eMethaneMethaneTarget = new MayerEGeneral(pMethaneMethaneGroup);
        MayerEGeneral eMethaneEthaneTarget = new MayerEGeneral(pMethaneEthaneGroup);
        MayerEGeneral eEthaneEthaneTarget = new MayerEGeneral(pEthaneEthaneGroup);
        int[] nTypes = new int[]{nMethane,nEthane};      
        ClusterAbstract targetCluster = Standard.virialClusterMixture(nPoints, new MayerFunction[][]{{fMethaneMethaneTarget,fMethaneEthaneTarget},{fMethaneEthaneTarget,fEthaneEthaneTarget}},
                new MayerFunction[][]{{eMethaneMethaneTarget,eMethaneEthaneTarget},{eMethaneEthaneTarget,eEthaneEthaneTarget}}, nTypes);
        targetCluster.setTemperature(temperature);
        
        ClusterAbstract refCluster = Standard.virialCluster(nPoints, fRef, nPoints>3, eRef, true);
        refCluster.setTemperature(temperature);

        System.out.println((steps*1000)+" steps ("+steps+" blocks of 1000)");
        //SpeciesFactorySiepmannSpheres speciesFactory = new SpeciesFactorySiepmannSpheres[2];
        //speciesFactory[1].setBondLength(bondL);
        //speciesFactory[0] = new SpeciesFactorySiepmannSpheres(space, 1);
        //speciesFactory[1] = new SpeciesFactorySiepmannSpheres(space, 2);
        
        SpeciesFactorySiepmannSpheres speciesFactoryEthane = new SpeciesFactorySiepmannSpheres(space,2);
        speciesFactoryEthane.setBondL(bondL);
                
        SpeciesFactorySiepmannSpheres speciesFactoryMethane = new SpeciesFactorySiepmannSpheres(space,1);
        
        SpeciesFactory[] speciesFactory = new SpeciesFactory[2];
        
        speciesFactory[0] = speciesFactoryMethane;
        speciesFactory[1] = speciesFactoryEthane;
        
        
        final SimulationVirialMultiOverlap sim = new SimulationVirialMultiOverlap(space, speciesFactory,
                          temperature,refCluster,targetCluster, new int[]{nMethane,nEthane} );
        //        sim.integratorOS.setAdjustStepFreq(false);
//        sim.integratorOS.setStepFreq0(1);

        SpeciesAlkane speciesCH4 = (SpeciesAlkane)sim.species[0];
        SpeciesAlkane speciesCH3 = (SpeciesAlkane)sim.species[1];
        IAtomTypeLeaf typeCH4 = speciesCH4.getChildType(0);
        IAtomTypeLeaf typeCH3 = speciesCH3.getChildType(0);
        pMethaneMethaneGroup.addPotential(p2CH4, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH4, typeCH4}));
        pMethaneEthaneGroup.addPotential(p2CH4CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH4, typeCH3}));
        pEthaneEthaneGroup.addPotential(p2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH3, typeCH3}));
        
        sim.integratorOS.setNumSubSteps(1000);
        
                               
        if (true) {
            sim.box[0].getBoundary().setDimensions(space.makeVector(new double[]{10,10,10}));
            sim.box[1].getBoundary().setDimensions(space.makeVector(new double[]{10,10,10}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
            simGraphic.getDisplayBox(sim.box[0]).setShowBoundary(false);
            simGraphic.getDisplayBox(sim.box[1]).setShowBoundary(false);
            
            ((IAtomTypeSphere)typeCH4).setDiameter(sigmaCH4);
            ((IAtomTypeSphere)typeCH3).setDiameter(sigmaCH3);
            simGraphic.makeAndDisplayFrame();

            sim.integratorOS.setNumSubSteps(1000);
            sim.setAccumulatorBlockSize(1000);
                
            // if running interactively, set filename to null so that it doens't read
            // (or write) to a refpref file
            sim.getController().removeAction(sim.ai);
            sim.getController().addAction(new IAction() {
                public void actionPerformed() {
                    sim.initRefPref(null, 100);
                    sim.equilibrate(null, 200);
                    sim.ai.setMaxSteps(Long.MAX_VALUE);
                }
            });
            sim.getController().addAction(sim.ai);
            if ((Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref) || sim.refPref == 0)) {
                throw new RuntimeException("Oops");
            }

            return;
        }
        
        // if running interactively, don't use the file
        String refFileName = args.length > 0 ? "refpref"+nPoints+"_"+temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, steps/40);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, steps/20);
        
        sim.setAccumulatorBlockSize((int)steps);
        
        System.out.println("equilibration finished");
        System.out.println("MC Move step sizes (ref)    "+sim.mcMoveTranslate[0].getStepSize()+" "
                +sim.mcMoveRotate[0].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[0].getStepSize())));
        System.out.println("MC Move step sizes (target) "+sim.mcMoveTranslate[1].getStepSize()+" "
                +sim.mcMoveRotate[1].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[1].getStepSize())));
        
        IAction progressReport = new IAction() {
            public void actionPerformed() {
                System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                double ratio = sim.dsvo.getDataAsScalar(); 
                double error = sim.dsvo.getError();
                System.out.println("abs average: "+ratio*HSB[nPoints]+", error: "+error*HSB[nPoints]);
            }
        };
        sim.integratorOS.addIntervalAction(progressReport);
        sim.integratorOS.setActionInterval(progressReport, (int)(steps/10));

        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();

        System.out.println("final reference step frequency "+sim.integratorOS.getStepFreq0());
        
        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("abs average: "+ratio*HSB[nPoints]+", error: "+error*HSB[nPoints]);
        DataGroup allYourBase = (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("hard sphere ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("hard sphere   average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("hard sphere overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints()-sim.dsvo.minDiffLocation()-1);
        System.out.println("chain ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("chain average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("chain overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
	}

    /**
     * Inner class for parameters
     */
    public static class VirialAlkaneMixParam extends ParameterBase {
        public int nPoints = 2;
        public int nSpheres1 = 1;   // methane
        public int nSpheres2 = 2;   // ethane 
        public double temperature = 300;   // Kelvin
        public long numSteps = 10000;
        public int nMethane = 2;
        public int nEthane = 0;
    }
}
