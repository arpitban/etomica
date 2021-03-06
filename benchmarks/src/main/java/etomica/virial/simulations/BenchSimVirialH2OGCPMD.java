package etomica.virial.simulations;

import etomica.action.IAction;
import etomica.action.MoleculeActionTranslateTo;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageCovariance;
import etomica.data.IData;
import etomica.data.histogram.HistogramNotSoSimple;
import etomica.data.histogram.HistogramSimple;
import etomica.data.types.DataGroup;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxCanvasG3DSys;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorEvent;
import etomica.integrator.IntegratorListener;
import etomica.math.DoubleRange;
import etomica.models.water.PNWaterGCPM;
import etomica.models.water.SpeciesWater4PCOM;
import etomica.potential.PotentialNonAdditiveDifference;
import etomica.simulation.BenchSimVirialLJ;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Space3D;
import etomica.units.CompoundUnit;
import etomica.units.Kelvin;
import etomica.units.Unit;
import etomica.util.ParseArgs;
import etomica.util.random.RandomMersenneTwister;
import etomica.virial.*;
import etomica.virial.cluster.Standard;
import etomica.virial.simulations.SimulationVirialOverlap2;
import etomica.virial.simulations.VirialH2OGCPMD;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
@State(Scope.Benchmark)
@Fork(1)
public class BenchSimVirialH2OGCPMD {

    SimulationVirialOverlap2 sim;

    @Setup(Level.Iteration)
    public void setUp() {
        VirialH2OGCPMD.VirialParam params = new VirialH2OGCPMD.VirialParam();
        params.nPoints = 5;
        params.nDer = 20;
        params.temperature = 800;
        params.numSteps = 1000000;
        params.sigmaHSRef = 5;
        params.nonAdditive = VirialH2OGCPMD.Nonadditive.TOTAL;
        params.seed = null;
        params.doHist = false;
        params.dorefpref = false;
        params.tol = 1e-12;
        sim = makeSim(params);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(time = 5, iterations = 1)
    @Measurement(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 3)
    public long integratorStep() {
        sim.integratorOS.doStep();
        return sim.integratorOS.getStepCount();
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(BenchSimVirialH2OGCPMD.class.getSimpleName())
                .jvmArgs()
                .addProfiler(StackProfiler.class)
                .build();

        new Runner(opts).run();
    }


    public static SimulationVirialOverlap2 makeSim(VirialH2OGCPMD.VirialParam params) {

        final int nPoints = params.nPoints;
        final int nDer = params.nDer;
        final double temperatureK = params.temperature;
        long steps = params.numSteps;
        double sigmaHSRef = params.sigmaHSRef;
        int[] seed = params.seed;
        boolean dorefpref = params.dorefpref;
        final double tol = params.tol;

        final double refFrac = params.refFrac;
        final VirialH2OGCPMD.Nonadditive nonAdditive = nPoints < 3 ? VirialH2OGCPMD.Nonadditive.NONE : params.nonAdditive;

        final double HSB = Standard.BHS(nPoints, sigmaHSRef);

        System.out.println("Overlap sampling for H2O GCPM at " + temperatureK + " K " + "for B"+nPoints+" and "+nDer+" derivatives");
        if (nonAdditive != VirialH2OGCPMD.Nonadditive.NONE) {
            System.out.println("Including induction");
        }

        double temperature = Kelvin.UNIT.toSim(temperatureK);

        final int precision = -3*(int)Math.log10(tol);

        final double BDAccFrac = 0.001;

        System.out.println("Reference diagram: B"+nPoints+" for hard spheres with diameter " + sigmaHSRef + " Angstroms");

        System.out.println("  B"+nPoints+"HS: "+HSB);

        final Space space = Space3D.getInstance();

        MayerHardSphere fRef = new MayerHardSphere(sigmaHSRef);

        SpeciesWater4PCOM speciesWater = new SpeciesWater4PCOM(space);

        final PNWaterGCPM pTarget = new PNWaterGCPM(space);

        MayerGeneral fTarget = new MayerGeneral(pTarget);

        ClusterAbstractMultivalue targetCluster = new ClusterWheatleySoftDerivatives(nPoints, fTarget, tol,nDer);
        ClusterAbstractMultivalue targetClusterBD = new ClusterWheatleySoftDerivativesBD(nPoints, fTarget, precision,nDer);

        if (nPoints==2) {
            // pure B2 for water.  we need flipping.
            // additive B3 for water should be fine and biconnectivity will help with mixture coefficients.
            ((ClusterWheatleySoftDerivatives)targetCluster).setTolerance(0);
            ((ClusterWheatleySoftDerivatives)targetCluster).setDoCaching(false);
            ((ClusterWheatleySoftDerivativesBD)targetClusterBD).setDoCaching(false);
            targetCluster = new ClusterCoupledFlippedMultivalue(targetCluster, targetClusterBD, space, 20, nDer, tol);
        }

        if (nonAdditive == VirialH2OGCPMD.Nonadditive.FULL || nonAdditive == VirialH2OGCPMD.Nonadditive.TOTAL) {
            PNWaterGCPM.PNWaterGCPMCached p2 = pTarget.makeCachedPairPolarization();
            PNWaterGCPM pFull = new PNWaterGCPM(space);
            pFull.setComponent(PNWaterGCPM.Component.INDUCTION);
            PotentialNonAdditiveDifference pnad = new PotentialNonAdditiveDifference(space, p2, pFull);
            MayerFunctionNonAdditiveFull fnad = new MayerFunctionNonAdditiveFull(pnad);
            targetCluster = new ClusterWheatleyMultibodyDerivatives(nPoints, fTarget,fnad, 0, nDer, nonAdditive == VirialH2OGCPMD.Nonadditive.TOTAL);
            targetClusterBD = new ClusterWheatleyMultibodyDerivativesBD(nPoints, fTarget,fnad,new MayerFunctionNonAdditive[0], precision, nDer, nonAdditive == VirialH2OGCPMD.Nonadditive.TOTAL);
            ((ClusterWheatleyMultibodyDerivatives)targetCluster).setRCut(100);
            ((ClusterWheatleyMultibodyDerivativesBD)targetClusterBD).setRCut(100);
            // water induction requires flipping
            ((ClusterWheatleyMultibodyDerivatives)targetCluster).setDoCaching(false);
            ((ClusterWheatleyMultibodyDerivativesBD)targetClusterBD).setDoCaching(false);
            targetCluster = new ClusterCoupledFlippedMultivalue(targetCluster, targetClusterBD, space, 20, nDer, tol);
        }

        ClusterMultiToSingle[] primes = new ClusterMultiToSingle[nDer];
        for(int m=0;m<primes.length;m++){
            primes[m]= new ClusterMultiToSingle(targetCluster, m+1);
        }

        targetCluster.setTemperature(temperature);

        ClusterWheatleyHS refCluster = new ClusterWheatleyHS(nPoints, fRef);

        System.out.println(steps+" steps (1000 IntegratorOverlap steps of "+(steps/1000)+")");

        final SimulationVirialOverlap2 sim = new SimulationVirialOverlap2(space, speciesWater, nPoints, temperature, refCluster, targetCluster);
        if(seed!=null)sim.setRandom(new RandomMersenneTwister(seed));
        if(targetCluster instanceof ClusterCoupledFlippedMultivalue) {
            ((ClusterCoupledFlippedMultivalue) targetCluster).setBDAccFrac(BDAccFrac,sim.getRandom());
        }
        sim.setExtraTargetClusters(primes);

        //No weighting for BD flipping
        if(nPoints > 4){
            double r=1000;
            double w=1;
            if(nPoints >5){
                r = 1000;
                w = 1;
            }
            ClusterWeight[] sampleclusters = sim.getSampleClusters();
            sampleclusters[1] = new VirialH2OGCPMD.Clusterfoo(targetCluster, r ,w);
            sim.setSampleClusters(sampleclusters);
        }

        sim.init();

        System.out.println("random seeds: "+Arrays.toString(seed==null?sim.getRandomSeeds():seed));
        System.out.println("Big Decimal Tolerance: " + tol);
        System.out.println("Big Decimal Acceptance Fraction: " + BDAccFrac);
        sim.integratorOS.setAggressiveAdjustStepFraction(true);

        if (nonAdditive != VirialH2OGCPMD.Nonadditive.NONE) {
            MoleculeActionTranslateTo act = new MoleculeActionTranslateTo(space);
            Vector pos = space.makeVector();
            double r = 4;
            for (int i=1; i<nPoints; i++) {
                double theta = 2*i*Math.PI/nPoints;
                pos.setX(0, r*(1-Math.cos(theta)));
                pos.setX(1, r*Math.sin(theta));
                act.setDestination(pos);
                act.actionPerformed(sim.box[1].getMoleculeList().get(i));
            }
            sim.box[1].trialNotify();
            sim.box[1].acceptNotify();
        }

        long t1 = System.currentTimeMillis();

        sim.integratorOS.setNumSubSteps(1000);

        if (refFrac >= 0) {
            sim.integratorOS.setRefStepFraction(refFrac);
            sim.integratorOS.setAdjustStepFraction(false);
        }

        steps /= 1000;
        sim.setAccumulatorBlockSize(steps);

        System.out.println();
        String refFileName = null;

        final HistogramNotSoSimple targHist = new HistogramNotSoSimple(70, new DoubleRange(-1, 8));
        final HistogramNotSoSimple targPiHist = new HistogramNotSoSimple(70, new DoubleRange(-1, 8));

        final HistogramSimple targHistr = new HistogramSimple(70, new DoubleRange(-1, 8));
        final HistogramSimple targHistBD = new HistogramSimple(70, new DoubleRange(-1, 8));

        int nBins = 100;
        double dx = sigmaHSRef/nBins;
        final HistogramNotSoSimple hist = new HistogramNotSoSimple(nBins, new DoubleRange(dx*0.5, sigmaHSRef+dx*0.5));
        final HistogramNotSoSimple piHist = new HistogramNotSoSimple(nBins, new DoubleRange(dx*0.5, sigmaHSRef+dx*0.5));
        final ClusterAbstract finalTargetCluster = targetCluster.makeCopy();
        IntegratorListener histListenerRef = new IntegratorListener() {
            public void integratorStepStarted(IntegratorEvent e) {}

            public void integratorStepFinished(IntegratorEvent e) {
                double r2Max = 0;
                CoordinatePairSet cPairs = sim.box[0].getCPairSet();
                for (int i=0; i<nPoints; i++) {
                    for (int j=i+1; j<nPoints; j++) {
                        double r2ij = cPairs.getr2(i, j);
                        if (r2ij > r2Max) r2Max = r2ij;
                    }
                }
                double v = finalTargetCluster.value(sim.box[0]);
                hist.addValue(Math.sqrt(r2Max), v);
                piHist.addValue(Math.sqrt(r2Max), Math.abs(v));
            }

            public void integratorInitialized(IntegratorEvent e) {
            }
        };
        IntegratorListener histListenerTarget = new IntegratorListener() {
            public void integratorStepStarted(IntegratorEvent e) {}

            public void integratorStepFinished(IntegratorEvent e) {
                double r2Max = 0;
                double r2Min = Double.POSITIVE_INFINITY;
                CoordinatePairSet cPairs = sim.box[1].getCPairSet();
                for (int i=0; i<nPoints; i++) {
                    for (int j=i+1; j<nPoints; j++) {
                        double r2ij = cPairs.getr2(i, j);
                        if (r2ij < r2Min) r2Min = r2ij;
                        if (r2ij > r2Max) r2Max = r2ij;
                    }
                }

                double v = finalTargetCluster.value(sim.box[1]);
                double r = Math.sqrt(r2Max);
                if (r > 1) {
                    r = Math.log(r);
                }
                else {
                    r -= 1;
                }
                targHist.addValue(r, v);
                targPiHist.addValue(r, Math.abs(v));

                targHistr.addValue(r);
                if( Math.abs(v)<tol){
                    targHistBD.addValue(r);
                }
            }

            public void integratorInitialized(IntegratorEvent e) {}
        };

        if (params.doHist) {

            final ClusterAbstractMultivalue tempcluster = targetCluster;
            long t11 = System.currentTimeMillis();

            IntegratorListener histReport = new IntegratorListener() {
                public void integratorInitialized(IntegratorEvent e) {}
                public void integratorStepStarted(IntegratorEvent e) {}
                public void integratorStepFinished(IntegratorEvent e) {
                    if ((sim.integratorOS.getStepCount()*100) % sim.ai.getMaxSteps() != 0) return;
                    System.out.println("**** reference ****");
                    double[] xValues = hist.xValues();
                    double[] h = hist.getHistogram();
                    double[] piH = piHist.getHistogram();
                    for (int i=0; i<xValues.length; i++) {
                        if (!Double.isNaN(h[i])) {
                            System.out.println(xValues[i]+" "+h[i]+" "+piH[i]);
                        }
                    }
                    System.out.println("**** target ****");
                    xValues = targHist.xValues();
                    h = targHist.getHistogram();
                    piH = targPiHist.getHistogram();
                    double[] hr = targHistr.getHistogram();
                    double [] hBD = targHistBD.getHistogram();
                    for (int i=0; i<xValues.length; i++) {
                        if (!Double.isNaN(h[i])) {
                            double r = xValues[i];
                            if (r < 0) r += 1;
                            else r = Math.exp(r);
                            System.out.println(r+" "+h[i]+" "+piH[i]+" "+ hr[i]+" "+hBD[i]);
                        }
                    }

                    if(nPoints!=2&&nonAdditive==VirialH2OGCPMD.Nonadditive.NONE ){
                        System.out.println("SoftBDcount: " + ((ClusterWheatleySoftDerivatives)tempcluster).getSoftBDcount() + " SoftBDfrac: " + ((ClusterWheatleySoftDerivatives)tempcluster).getSoftBDfrac() + " Softcount: " + ((ClusterWheatleySoftDerivatives)tempcluster).getSoftcount());
                    }
                    else{
                        ClusterCoupledFlippedMultivalue foo = (ClusterCoupledFlippedMultivalue)tempcluster;
                        System.out.println("BDcount: " + foo.getBDcount() + " BDfrac: " + foo.getBDfrac() + " totBDcount: " + foo.getBDtotcount());
                        System.out.println("FlipCount: " + foo.getflipcount() + " Flipfrac: " + foo.getflipfrac() + " FlipTotcount: " + foo.gettotcount());
                        xValues= foo.histe.xValues();
                        h=foo.histe.getHistogram();
                        for (int i=0; i<xValues.length; i++) {
                            if (h[i]!=0) {
                                System.out.println(Math.exp(xValues[i]) + " " + h[i]);
                            }
                        }
                    }
                    System.out.println("time: "+(System.currentTimeMillis()-t11)/1000.0);
                }
            };
            sim.integratorOS.getEventManager().addListener(histReport);

            System.out.println("collecting histograms");
            // only collect the histogram if we're forcing it to run the reference system
            sim.integrators[0].getEventManager().addListener(histListenerRef);
            sim.integrators[1].getEventManager().addListener(histListenerTarget);
        }

        sim.initRefPref(refFileName, steps/20);
        sim.equilibrate(refFileName, steps/10);

        System.out.println("equilibration finished");

        sim.integratorOS.setNumSubSteps((int)steps);
        sim.ai.setMaxSteps(1000);
        for (int i=0; i<2; i++) {
            System.out.println("MC Move step sizes "+sim.mcMoveTranslate[i].getStepSize()+" "+sim.mcMoveRotate[i].getStepSize());
        }
        return sim;
    }
}
