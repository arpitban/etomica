package etomica.virial.simulations;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import etomica.action.IAction;
import etomica.api.IAtomType;
import etomica.api.IIntegratorEvent;
import etomica.api.IIntegratorListener;
import etomica.api.ISpecies;
import etomica.atom.DiameterHashByType;
import etomica.atom.iterator.ApiBuilder;
import etomica.atom.iterator.ApiIndexList;
import etomica.atom.iterator.Atomset3IteratorIndexList;
import etomica.atom.iterator.Atomset4IteratorIndexList;
import etomica.data.AccumulatorAverageCovariance;
import etomica.data.DataPumpListener;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.graph.model.Graph;
import etomica.graphics.ColorSchemeRandomByMolecule;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxCanvasG3DSys;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P2LennardJones;
import etomica.potential.P3BondAngle;
import etomica.potential.P4BondTorsion;
import etomica.potential.PotentialGroup;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.CompoundDimension;
import etomica.units.CompoundUnit;
import etomica.units.Dimension;
import etomica.units.DimensionRatio;
import etomica.units.Kelvin;
import etomica.units.Liter;
import etomica.units.Mole;
import etomica.units.Pixel;
import etomica.units.Quantity;
import etomica.units.Unit;
import etomica.units.UnitRatio;
import etomica.units.Volume;
import etomica.util.Constants.CompassDirection;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterBonds;
import etomica.virial.ClusterSum;
import etomica.virial.ClusterSumShell;
import etomica.virial.ClusterWeight;
import etomica.virial.ClusterWeightAbs;
import etomica.virial.MCMoveClusterMoleculeMulti;
import etomica.virial.MCMoveClusterRotateMoleculeMulti;
import etomica.virial.MCMoveClusterTorsionMulti;
import etomica.virial.MayerGeneral;
import etomica.virial.MayerHardSphere;
import etomica.virial.MeterVirial;
import etomica.virial.SpeciesAlkane;
import etomica.virial.cluster.Standard;
import etomica.virial.cluster.VirialDiagrams;

/**
 * Mayer sampling simulation for alkanes using the TraPPE force field.
 *   M.G. Martin and J.I. Siepmann, "Transferable Potentials for Phase
 *   Equilibria. 1. United-Atom Description of n-Alkanes," J. Phys. Chem. B
 *   102, 2569-2577 (1998)
 */
public class VirialAlkaneFlex2 {


    public static void main(String[] args) {
        VirialSiepmannSpheresParam params = new VirialSiepmannSpheresParam();
        boolean isCommandline = args.length > 0;
        if (isCommandline) {
            ParseArgs parseArgs = new ParseArgs(params);
            parseArgs.parseArgs(args, true);
        }
        else {
            params.nPoints = 3;
            params.nSpheres = 3;
            params.numSteps = 10000000;
            params.temperature = 500;
        }
        final int nPoints = params.nPoints;
        int nSpheres = params.nSpheres;
        double temperature = params.temperature;
        long steps = params.numSteps;
        double refFreq = params.refFreq;
        double sigmaCH2 = 3.95;
        double sigmaCH3 = 3.75;
        double sigmaHSRef = sigmaCH3 + 0.5*nSpheres;
        final double[] HSB = new double[8];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);
        HSB[7] = Standard.B7HS(sigmaHSRef);
		
        Space space = Space3D.getInstance();
        
        MayerHardSphere fRef = new MayerHardSphere(sigmaHSRef);
        PotentialGroup pTargetGroup = new PotentialGroup(2);
        System.out.println("Siepman "+nSpheres+"-mer chains B"+nPoints+" flexible correction at "+temperature+"K");
        temperature = Kelvin.UNIT.toSim(temperature);
        double epsilonCH2 = Kelvin.UNIT.toSim(46.0);
        double epsilonCH3 = Kelvin.UNIT.toSim(98.0);
        double epsilonCH2CH3 = Math.sqrt(epsilonCH2*epsilonCH3);
        P2LennardJones p2CH2 = new P2LennardJones(space, sigmaCH2, epsilonCH2);
        P2LennardJones p2CH3 = new P2LennardJones(space, sigmaCH3, epsilonCH3);
        P2LennardJones p2CH2CH3 = new P2LennardJones(space, 0.5*(sigmaCH2+sigmaCH3), epsilonCH2CH3);
        
        MayerGeneral fTarget = new MayerGeneral(pTargetGroup);

        boolean alkaneFlex = nSpheres > 2 && nPoints > 2;
        VirialDiagrams alkaneDiagrams = new VirialDiagrams(nPoints, false, alkaneFlex);
        ClusterSum targetCluster = alkaneDiagrams.makeVirialCluster(fTarget);

        VirialDiagrams rigidDiagrams = new VirialDiagrams(nPoints, false, false);
        ClusterSum refCluster = rigidDiagrams.makeVirialCluster(fRef);

        double refIntegral = HSB[nPoints];

        ClusterSumShell[] targetDiagrams = new ClusterSumShell[0];
        int[] targetDiagramNumbers = new int[0];
        if (nSpheres > 2) {
            targetDiagrams = alkaneDiagrams.makeSingleVirialClusters(targetCluster, null, fTarget);
            targetDiagramNumbers = new int[targetDiagrams.length];

            System.out.println("individual clusters:");
            Set<Graph> singleGraphs = alkaneDiagrams.getMSMCGraphs(true, false);
            Map<Graph,Graph> cancelMap = alkaneDiagrams.getCancelMap();
            int iGraph = 0;
            for (Graph g : singleGraphs) {
                System.out.print(iGraph+" ("+g.coefficient()+") "+g.getStore().toNumberString());
                targetDiagramNumbers[iGraph] = Integer.parseInt(g.getStore().toNumberString());
                Graph cg = cancelMap.get(g);
                if (cg != null) {
                    System.out.print(" - "+cg.getStore().toNumberString());
                }
                System.out.println();
                iGraph++;
            }
            System.out.println();
            Set<Graph> disconnectedGraphs = alkaneDiagrams.getExtraDisconnectedVirialGraphs();
            if (disconnectedGraphs.size() > 0) {
                System.out.println("extra clusters:");
                
                for (Graph g : disconnectedGraphs) {
                    Set<Graph> gSplit = alkaneDiagrams.getSplitDisconnectedVirialGraphs(g);
                    System.out.print(g.coefficient()+" ");
                    boolean first = true;
                    for (Graph gs : gSplit) {
                        if (!first) {
                            System.out.print("*");
                        }
                        System.out.print(gs.getStore().toNumberString());
                        first = false;
                    }
                    System.out.println();
                }
                System.out.println();
            }
        }

        targetCluster.setTemperature(temperature);
        refCluster.setTemperature(temperature);
        for (int i=0; i<targetDiagrams.length; i++) {
            targetDiagrams[i].setTemperature(temperature);
        }

        System.out.println("sigmaHSRef: "+sigmaHSRef);
        // overerr expects this string, BnHS
        System.out.println("B"+nPoints+"HS: "+refIntegral);
        System.out.println(steps+" steps (1000 blocks of "+steps/1000+")");
        ClusterWeight[] sampleClusters = new ClusterWeight[]{ClusterWeightAbs.makeWeightCluster(refCluster), ClusterWeightAbs.makeWeightCluster(targetCluster)};

        SpeciesAlkane species = new SpeciesAlkane(space, nSpheres);

        final SimulationVirialOverlap2 sim = new SimulationVirialOverlap2(space,new ISpecies[]{species},
                new int[]{nPoints+1},temperature, new ClusterAbstract[]{refCluster, targetCluster}, sampleClusters, true);
        sim.integratorOS.setAggressiveAdjustStepFraction(true);

        if (alkaneFlex) {
            int[] constraintMap = new int[nPoints+1];
            for (int i=0; i<nPoints; i++) {
                constraintMap[i] = i;
            }
            constraintMap[nPoints] = 0;
            ((MCMoveClusterMoleculeMulti)sim.mcMoveTranslate[0]).setConstraintMap(constraintMap);
            ((MCMoveClusterMoleculeMulti)sim.mcMoveTranslate[1]).setConstraintMap(constraintMap);
            ((MCMoveClusterRotateMoleculeMulti)sim.mcMoveRotate[0]).setConstraintMap(constraintMap);
            ((MCMoveClusterRotateMoleculeMulti)sim.mcMoveRotate[1]).setConstraintMap(constraintMap);
        }
//        ((MCMoveStepTracker)sim.mcMoveTranslate[0].getTracker()).setNoisyAdjustment(true);
//        ((MCMoveStepTracker)sim.mcMoveTranslate[1].getTracker()).setNoisyAdjustment(true);
        if (refFreq >= 0) {
            sim.integratorOS.setAdjustStepFraction(false);
            sim.integratorOS.setRefStepFraction(refFreq);
        }

        IAtomType typeCH3 = species.getAtomType(0);
        IAtomType typeCH2 = species.getAtomType(1);
        pTargetGroup.addPotential(p2CH2, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeCH2, typeCH2}));
        // CH2 on molecule1 to CH3 on molecule2
        pTargetGroup.addPotential(p2CH2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeCH2, typeCH3}));
        pTargetGroup.addPotential(p2CH2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeCH3, typeCH2}));
        pTargetGroup.addPotential(p2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{typeCH3, typeCH3}));
        
        sim.integratorOS.setNumSubSteps(1000);

        // create the intramolecular potential here, add to it and add it to
        // the potential master if needed
        PotentialGroup pIntra = sim.integrators[1].getPotentialMaster().makePotentialGroup(1);
        if (nSpheres > 2) {
            P3BondAngle p3 = new P3BondAngle(space);
            p3.setAngle(Math.PI*114.0/180.0);
            p3.setEpsilon(Kelvin.UNIT.toSim(62500));
            int[][] triplets = new int[nSpheres-2][3];
            for (int i=0; i<nSpheres-2; i++) {
                triplets[i][0] = i;
                triplets[i][1] = i+1;
                triplets[i][2] = i+2;
            }
            pIntra.addPotential(p3, new Atomset3IteratorIndexList(triplets));
            // integrators share a common potentialMaster.  so just add to one
            sim.integrators[1].getPotentialMaster().addPotential(pIntra,new ISpecies[]{sim.getSpecies(0)});
        }
        MCMoveClusterTorsionMulti[] torsionMoves = null;
        if (nSpheres > 3) {
            P4BondTorsion p4 = new P4BondTorsion(space, 0, Kelvin.UNIT.toSim(355.03), Kelvin.UNIT.toSim(-68.19), Kelvin.UNIT.toSim(791.32));
            int[][] quads = new int[nSpheres-3][4];
            for (int i=0; i<nSpheres-3; i++) {
                quads[i][0] = i;
                quads[i][1] = i+1;
                quads[i][2] = i+2;
                quads[i][3] = i+3;
            }
            pIntra.addPotential(p4, new Atomset4IteratorIndexList(quads));
            torsionMoves = new MCMoveClusterTorsionMulti[2];
            torsionMoves[0] = new MCMoveClusterTorsionMulti(sim.integrators[1].getPotentialMaster(), space, sim.getRandom(), 1.0, p4, 40);
            torsionMoves[0].setTemperature(temperature);
            sim.integrators[0].getMoveManager().addMCMove(torsionMoves[0]);
            torsionMoves[1] = new MCMoveClusterTorsionMulti(sim.integrators[1].getPotentialMaster(), space, sim.getRandom(), 1.0, p4, 40);
            torsionMoves[1].setTemperature(temperature);
            sim.integrators[1].getMoveManager().addMCMove(torsionMoves[1]);
        }
        if (nSpheres > 4) {
            pIntra.addPotential(p2CH3,new ApiIndexList(new int[][]{{0,nSpheres-1}}));
        }
        if (nSpheres > 5) {
            int[][] pairs = new int[2*(nSpheres-5)][2];
            for (int i=0; i<nSpheres-5; i++) {
                pairs[2*i][0] = 0;
                pairs[2*i][1] = nSpheres-2-i;
                pairs[2*i+1][0] = nSpheres-1;
                pairs[2*i+1][1] = i+1;
            }
            pIntra.addPotential(p2CH2CH3,new ApiIndexList(pairs));
        }
        if (nSpheres > 6) {
            int[][] pairs = new int[(nSpheres-6)*(nSpheres-5)/2][2];
            int k = 0;
            for (int i=1; i<nSpheres-5; i++) {
                for (int j=i+4; j<nSpheres-1; j++) {
                    pairs[k][0] = i;
                    pairs[k][1] = j;
                    k++;
                }
            }
            pIntra.addPotential(p2CH2,new ApiIndexList(pairs));
        }

        if (false) {
            double size = (nSpheres+5)*1.5;
            sim.box[0].getBoundary().setBoxSize(space.makeVector(new double[]{size,size,size}));
            sim.box[1].getBoundary().setBoxSize(space.makeVector(new double[]{size,size,size}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
            DisplayBox displayBox0 = simGraphic.getDisplayBox(sim.box[0]); 
            DisplayBox displayBox1 = simGraphic.getDisplayBox(sim.box[1]);
            displayBox0.setPixelUnit(new Pixel(300.0/size));
            displayBox1.setPixelUnit(new Pixel(300.0/size));
            displayBox0.setShowBoundary(false);
            displayBox1.setShowBoundary(false);
            ((DisplayBoxCanvasG3DSys)displayBox0.canvas).setBackgroundColor(Color.WHITE);
            ((DisplayBoxCanvasG3DSys)displayBox1.canvas).setBackgroundColor(Color.WHITE);
            
            
            DiameterHashByType diameterManager = (DiameterHashByType)displayBox0.getDiameterHash();
            diameterManager.setDiameter(typeCH2, 0.5*sigmaCH2);
            diameterManager.setDiameter(typeCH3, 0.5*sigmaCH3);
            displayBox1.setDiameterHash(diameterManager);
            ColorSchemeRandomByMolecule colorScheme = new ColorSchemeRandomByMolecule(sim, sim.box[0], sim.getRandom());
            displayBox0.setColorScheme(colorScheme);
            colorScheme = new ColorSchemeRandomByMolecule(sim, sim.box[1], sim.getRandom());
            displayBox1.setColorScheme(colorScheme);
            simGraphic.makeAndDisplayFrame();

            sim.integratorOS.setNumSubSteps(1000);
            sim.setAccumulatorBlockSize(1000);
                
            // if running interactively, set filename to null so that it doens't read
            // (or write) to a refpref file
            sim.getController().removeAction(sim.ai);
            sim.getController().addAction(new IAction() {
                public void actionPerformed() {
                    sim.initRefPref(null, 10);
                    sim.equilibrate(null, 20);
                    sim.ai.setMaxSteps(Long.MAX_VALUE);
                }
            });
            sim.getController().addAction(sim.ai);
            if ((Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref) || sim.refPref == 0)) {
                throw new RuntimeException("Oops");
            }
            
            final DisplayTextBox averageBox = new DisplayTextBox();
            averageBox.setLabel("Average");
            final DisplayTextBox errorBox = new DisplayTextBox();
            errorBox.setLabel("Error");
            JLabel jLabelPanelParentGroup = new JLabel("B"+nPoints+" (L/mol)^"+(nPoints-1));
            final JPanel panelParentGroup = new JPanel(new java.awt.BorderLayout());
            panelParentGroup.add(jLabelPanelParentGroup,CompassDirection.NORTH.toString());
            panelParentGroup.add(averageBox.graphic(), java.awt.BorderLayout.WEST);
            panelParentGroup.add(errorBox.graphic(), java.awt.BorderLayout.EAST);
            simGraphic.getPanel().controlPanel.add(panelParentGroup, SimulationPanel.getVertGBC());
            
            IAction pushAnswer = new IAction() {
                public void actionPerformed() {
                    double[] ratioAndError = sim.dvo.getAverageAndError();
                    double ratio = ratioAndError[0];
                    double error = ratioAndError[1];
                    data.x = ratio;
                    averageBox.putData(data);
                    data.x = error;
                    errorBox.putData(data);
                }
                
                DataDouble data = new DataDouble();
            };
            IEtomicaDataInfo dataInfo = new DataDouble.DataInfoDouble("B"+nPoints, new CompoundDimension(new Dimension[]{new DimensionRatio(Volume.DIMENSION, Quantity.DIMENSION)}, new double[]{nPoints-1}));
            Unit unit = new CompoundUnit(new Unit[]{new UnitRatio(Liter.UNIT, Mole.UNIT)}, new double[]{nPoints-1});
            averageBox.putDataInfo(dataInfo);
            averageBox.setLabel("average");
            averageBox.setUnit(unit);
            errorBox.putDataInfo(dataInfo);
            errorBox.setLabel("error");
            errorBox.setPrecision(2);
            errorBox.setUnit(unit);
            sim.integratorOS.getEventManager().addListener(new IntegratorListenerAction(pushAnswer));
            
            return;
        }
        
        // if running interactively, don't use the file
        String refFileName = isCommandline ? "refpref"+nPoints+"_"+temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, (steps/1000)/40);

        MeterVirial meterDiagrams = new MeterVirial(targetDiagrams);
        meterDiagrams.setBox(sim.box[1]);
        AccumulatorAverageCovariance accumulatorDiagrams = null;
        if (nSpheres > 2) {
            accumulatorDiagrams = new AccumulatorAverageCovariance(steps);
            DataPumpListener pumpDiagrams = new DataPumpListener(meterDiagrams, accumulatorDiagrams);
            sim.integrators[1].getEventManager().addListener(pumpDiagrams);
        }
        
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, (steps/1000)/20);

        if (nSpheres > 2) {
            accumulatorDiagrams.reset();
            accumulatorDiagrams.setBlockSize(steps/1000);
        }

        sim.setAccumulatorBlockSize(steps/1000);
        
        System.out.println("equilibration finished");
        System.out.println("MC Move step sizes (ref)    "+sim.mcMoveTranslate[0].getStepSize()+" "
                +sim.mcMoveRotate[0].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[0].getStepSize())));
        System.out.println("MC Move step sizes (target) "+sim.mcMoveTranslate[1].getStepSize()+" "
                +sim.mcMoveRotate[1].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[1].getStepSize())));
        if (nSpheres > 3) {
            System.out.println("Torsion move acceptance "+torsionMoves[0].getTracker().acceptanceRatio()+" "+
                    torsionMoves[1].getTracker().acceptanceRatio());
        }

        if (false) {
            final double refIntegralF = refIntegral;
            IIntegratorListener progressReport = new IIntegratorListener() {
                public void integratorInitialized(IIntegratorEvent e) {}
                public void integratorStepStarted(IIntegratorEvent e) {}
                public void integratorStepFinished(IIntegratorEvent e) {
                    if ((sim.integratorOS.getStepCount()*10) % sim.ai.getMaxSteps() != 0) return;
                    System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                    double[] ratioAndError = sim.dvo.getAverageAndError();
                    double ratio = ratioAndError[0];
                    double error = ratioAndError[1];
                    System.out.println("abs average: "+ratio*refIntegralF+", error: "+error*refIntegralF);
                }
            };
            sim.integratorOS.getEventManager().addListener(progressReport);
        }

        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.integratorOS.setNumSubSteps((int)(steps/1000));
        sim.ai.setMaxSteps(1000);
        long t1 = System.currentTimeMillis();
        sim.getController().actionPerformed();
        long t2 = System.currentTimeMillis();

        System.out.println("final reference step frequency "+sim.integratorOS.getIdealRefStepFraction());
        System.out.println("actual reference step frequency "+sim.integratorOS.getRefStepFraction());

        sim.printResults(refIntegral);
        
        for (int i=0; i<targetDiagrams.length; i++) {
            DataGroup allData = (DataGroup)accumulatorDiagrams.getData();
            IData dataAvg = allData.getData(accumulatorDiagrams.AVERAGE.index);
            IData dataErr = allData.getData(accumulatorDiagrams.ERROR.index);
            System.out.print("diagram "+targetDiagramNumbers[i]+" ");
            System.out.print("average: "+dataAvg.getValue(i)+" error: "+dataErr.getValue(i));
            if (targetDiagrams.length > 1) {
                System.out.print(" cov:");
                IData dataCov = allData.getData(accumulatorDiagrams.BLOCK_COVARIANCE.index);
                double ivar = dataCov.getValue(i*targetDiagrams.length+i);
                for (int j=0; j<targetDiagrams.length; j++) {
                    if (i==j) continue;
                    double jvar = dataCov.getValue(j*targetDiagrams.length+j);
                    System.out.print(" "+dataCov.getValue(i*targetDiagrams.length+j)/Math.sqrt(ivar*jvar));
                }
            }
            System.out.println();
        }
        System.out.print(String.format("time: %9.3f\n", (t2-t1)*0.001));
	}
    
    public static ClusterBonds[] append(ClusterBonds[] inArray, ClusterBonds[] newBonds) {
        ClusterBonds[] outArray = new ClusterBonds[inArray.length + newBonds.length];
        System.arraycopy(inArray, 0, outArray, 0, inArray.length);
        System.arraycopy(newBonds, 0, outArray, inArray.length, newBonds.length);
        return outArray;
    }

    public static double[] append(double[] inArray, double[] newWeights) {
        double[] outArray = new double[inArray.length + newWeights.length];
        System.arraycopy(inArray, 0, outArray, 0, inArray.length);
        System.arraycopy(newWeights, 0, outArray, inArray.length, newWeights.length);
        return outArray;
    }

    /**
     * Inner class for parameters
     */
    public static class VirialSiepmannSpheresParam extends ParameterBase {
        public int nPoints = 2;
        public int nSpheres = 3;
        public double temperature = 300.0;   // Kelvin
        public long numSteps = 10000;
        public double refFreq = -1;
    }
}
