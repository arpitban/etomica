/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.spin.heisenberg_interacting.heisenberg;

import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomType;
import etomica.box.Box;
import etomica.chem.elements.ElementSimple;
import etomica.data.*;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveRotate;
import etomica.listener.IntegratorListenerAction;
import etomica.nbr.site.PotentialMasterSite;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.species.SpeciesSpheresMono;
import etomica.species.SpeciesSpheresRotating;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.util.random.RandomNumberGenerator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * Compute  the dielectric constant of  2D interacting Heisenberg model in conventional way and mapped averaging.
 * Conventional way: get the -bt*bt*<M^2> with M is the total dipole moment;
 * Mapped averaging: get the A_EE secondDerivative of free energy w.r.t electric field E when E is zero
 * Prototype for simulation of a more general magnetic system.
 *
 * @author Weisong Lin
 */
public class HeisenbergPair extends Simulation {

    private static final String APP_NAME = "Heisenberg";
    public final ActivityIntegrate activityIntegrate;
    public Box box;
    public SpeciesSpheresMono spins;
    public P2Spin potential;
    public P1MagneticField field;
    public MCMoveRotatePair mcMove;
    private IntegratorMC integrator;

    /**
     * 2D heisenberg model in square lattice.
     *
     * @param space           use to define vector/tensor
     * @param interactionS    the J in heisenberg energy function: U = J*Cos(theta1-theta2)
     * @param dipoleMagnitude is the strength of heisenberg dipole.
     */
    public HeisenbergPair(Space space, double temperature, double interactionS, double dipoleMagnitude) {
        super(Space2D.getInstance());
//        setRandom(new RandomNumberGenerator(1)); //debug only
//        System.out.println("============================the RandomSeed is one ===========================");

        box = new Box(space);
        addBox(box);
        int numAtoms = 2;

        spins = new SpeciesSpheresRotating(space, new ElementSimple("A"));

        addSpecies(spins);
        box.setNMolecules(spins, numAtoms);

        potential = new P2Spin(space, interactionS);
        // the electric field is zero for now, maybe need to add electric field in the future.
        field = new P1MagneticField(space, dipoleMagnitude);
        integrator = new IntegratorMC(this, null);
        mcMove = new MCMoveRotatePair(potential, random, space);
        integrator.getMoveManager().addMCMove(mcMove);
        integrator.setTemperature(temperature);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        AtomType type = spins.getLeafType();
//        potentialMaster.addPotential(field, new IAtomType[] {type});

        integrator.setBox(box);

    }

    public static void main(String[] args) {
        Param params = new Param();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        } else {

        }
        final long startTime = System.currentTimeMillis();
        DateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        System.out.println("startTime : " + date.format(cal.getTime()));

        double temperature = params.temperature;
        int numberMolecules = 2;
        boolean aEE = params.aEE;
        boolean mSquare = params.mSquare;
        int steps = params.steps;
        double interactionS = params.interactionS;
        double dipoleMagnitude = params.dipoleMagnitude;

        System.out.println("steps= " + steps);
        System.out.println("numberMolecules= " + numberMolecules);
        System.out.println("temperature= " + temperature);

        Space sp = Space2D.getInstance();
        HeisenbergPair sim = new HeisenbergPair(sp, temperature, interactionS, dipoleMagnitude);

        MeterSpinMSquare meterMSquare = null;
        AccumulatorAverage dipoleSumSquaredAccumulator = null;

        sim.activityIntegrate.setMaxSteps(steps / 5);
        sim.getController().actionPerformed();
        sim.getController().reset();
        int blockNumber = 100;


        int sampleAtInterval = numberMolecules;
        int samplePerBlock = steps / sampleAtInterval / blockNumber;
        System.out.println("number of blocks is : " + blockNumber);
        System.out.println("sample per block is : " + samplePerBlock);
        System.out.println("number of molecules are: " + 2);
        System.out.println("interacitonS= " + interactionS);
        System.out.println("dipoleStrength= " + dipoleMagnitude);

        System.out.println("equilibration finished");
        long equilibrationTime = System.currentTimeMillis();
        System.out.println("equilibrationTime: " + (equilibrationTime - startTime) / (1000.0 * 60.0));

        //mSquare
        if (mSquare) {
            meterMSquare = new MeterSpinMSquare(sim.space, sim.box, dipoleMagnitude);
            dipoleSumSquaredAccumulator = new AccumulatorAverageFixed(samplePerBlock);
            DataPump dipolePump = new DataPump(meterMSquare, dipoleSumSquaredAccumulator);
            IntegratorListenerAction dipoleListener = new IntegratorListenerAction(dipolePump);
            dipoleListener.setInterval(sampleAtInterval);
            sim.integrator.getEventManager().addListener(dipoleListener);
        }


        //AEE
        MeterMappedAveragingPair AEEMeter = null;
        AccumulatorAverageCovariance AEEAccumulator = null;
        if (aEE) {
            AEEMeter = new MeterMappedAveragingPair(sim.space, sim.box, sim, temperature, interactionS, dipoleMagnitude, sim.potential);
            AEEAccumulator = new AccumulatorAverageCovariance(samplePerBlock, true);
            DataPump AEEPump = new DataPump(AEEMeter, AEEAccumulator);
            IntegratorListenerAction AEEListener = new IntegratorListenerAction(AEEPump);
            AEEListener.setInterval(sampleAtInterval);
            sim.integrator.getEventManager().addListener(AEEListener);
        }

        sim.activityIntegrate.setMaxSteps(steps);
        sim.getController().actionPerformed();


        //******************************** simulation start ******************************** //
        double dipoleSumSquared = 0;
        double dipoleSumSquaredERR = 0;
        double dipoleSumCor = 0;
        if (mSquare) {
            dipoleSumSquared = ((DataDouble) ((DataGroup) dipoleSumSquaredAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index)).x;
            dipoleSumSquaredERR = ((DataDouble) ((DataGroup) dipoleSumSquaredAccumulator.getData()).getData(AccumulatorAverage.ERROR.index)).x;
            dipoleSumCor = ((DataDouble) ((DataGroup) dipoleSumSquaredAccumulator.getData()).getData(AccumulatorAverage.BLOCK_CORRELATION.index)).x;
        }


        double AEE = 0;
        double AEEER = 0;
        double AEECor = 0;
//        double avgdipole = 0;
//        double errdipole = 0;
//        double avgJEEMJEJE = 0;
//        double errJEEMJEJE = 0;
//        double avgUEE = 0;
//        double errUEE = 0;
//        double avgJEMUE = 0;
//        double errJEMUE = 0;
//        double avgJEMUEsquare = 0;
//        double errJEMUEsquare = 0;
//        double avgdipoleconv = 0;
//        double errdipoleconv = 0;

        if (aEE) {

//            AEE = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(0);
//            AEEER = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(0);
//            AEECor = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.BLOCK_CORRELATION.index).getValue(0);
//            IData covariance = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverageCovariance.BLOCK_COVARIANCE.index);
//            covariance.getValue(1);


//            avgdipole = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(2);
//            errdipole = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(2);
//            avgJEMUE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(4);
//            errJEMUE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(4);
//            avgJEMUEsquare=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(3);
//            errJEMUEsquare=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(3);
//            avgJEEMJEJE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(0);
//            errJEEMJEJE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(0);
//            avgUEE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(1);
//            errUEE=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(1);
//            avgdipoleconv=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(5);
//            errdipoleconv=((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(5);
//            AEE = -avgJEEMJEJE+avgUEE-avgJEMUEsquare+(avgJEMUE*avgJEMUE);

            double sum0 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(0);
            double errSum0 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(0);
            double sum1 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(1);
            double errSum1 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(1);

            double sum2 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(2);
            double errSum2 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(2);

            double sum3 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(3);
            double ERsum3 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(3);
            double sum4 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(4);
            double ERsum4 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(4);


//TODO
            IData covariance = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverageCovariance.BLOCK_COVARIANCE.index);
            covariance.getValue(1);
            AEE = sum0 + sum1 * sum1 + sum2 * sum2;
            AEEER = Math.sqrt(errSum0 * errSum0 + 4 * sum1 * sum1 * errSum1 * errSum1 -
                    2 * errSum0 * sum1 * 2 * errSum1 * covariance.getValue(1) / Math.sqrt(covariance.getValue(0) * covariance.getValue(6)));
            AEECor = covariance.getValue(1) / Math.sqrt(covariance.getValue(0) * covariance.getValue(6));

//            AEE = sum0 + sum1 * sum1 + sum2 * sum2 - sum3 * sum3 - sum4 * sum4;
//            AEEER = errSum0;
//            AEECor = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.BLOCK_CORRELATION.index).getValue(0);
        }

        long endTime = System.currentTimeMillis();

//        double totalTime = (endTime - startTime) / (1000.0 * 60.0);
//        if (mSquare) {
//            System.out.println("-<M^2>*bt*bt:\t" + (-dipoleSumSquared / temperature / temperature / numberMolecules)
//                    + " mSquareErr:\t" + (dipoleSumSquaredERR / temperature / temperature / numberMolecules)
//                    + " mSquareDifficulty:\t" + (dipoleSumSquaredERR / temperature / temperature / numberMolecules) * Math.sqrt(totalTime)
//                    + " dipolesumCor= " + dipoleSumCor);
//            System.out.println("mSquare_Time: " + (endTime - startTime) / (1000.0 * 60.0));
//        }
//
//        if (aEE) {
//            System.out.println("AEE_new:\t" + (AEE / numberMolecules)
//                    + " AEEErr:\t" + (AEEER / numberMolecules)
//                    + " AEEDifficulty:\t" + (AEEER * Math.sqrt(totalTime) / numberMolecules)
//                    + " AEECor= " + AEECor);
//            System.out.println("AEE_Time: " + (endTime - startTime) / (1000.0 * 60.0));
//            System.out.println("avgdipole: " + avgdipole);
//            System.out.println("JEEMJEJE: " + avgJEEMJEJE + "  errJEEMJEJE: " + errJEEMJEJE + "  UEE: " + avgUEE + "  errUEE: " + errUEE + "  JEMUE: " + avgJEMUE + "  errJEMUE: " + errJEMUE + "  JEMUEsq: " + avgJEMUEsquare + "  errJEMUEsq: " + errJEMUEsquare + "  dipoleconv: " + avgdipoleconv + "  errdipoleconv: " + errdipoleconv + "  dipolemap: " + avgdipole + "  errdipolemap: " + errdipole);
//
//
//        }


        double sum0 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(0);
        double errSum0 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(0);
        double sum1 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(1);
        double errSum1 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(1);
        double sum2 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(2);
        double errSum2 = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(2);

        IData covariance = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverageCovariance.BLOCK_COVARIANCE.index);
        covariance.getValue(1);
        double test = sum0;
        double errTest = Math.sqrt(errSum0 * errSum0 + 4 * sum1 * sum1 * errSum1 * errSum1 -
                2 * errSum0 * sum1 * 2 * errSum1 * covariance.getValue(1) / Math.sqrt(covariance.getValue(0) * covariance.getValue(11)));
        double corTest = covariance.getValue(1) / Math.sqrt(covariance.getValue(0) * covariance.getValue(11));



        System.out.println(" AEEDirect= " + test / numberMolecules + " Err " + errSum0/numberMolecules );


//        double JEMUEx = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(1);
//        double errJEMUEx = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(1);
//        double JEMUEy = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(2);
//        double errJEMUEy = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(2);
//
//        double JEMUExSquare = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(8);
//        double errJEMUExSquare = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(8);
//        double JEMUEySquare = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(9);
//        double errJEMUEySquare = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(9);
//
//
//        System.out.println("JEMUEx= " + JEMUEx + " Err " + errJEMUEx);
//        System.out.println("JEMUEy= " + JEMUEy + " Err " + errJEMUEy);
//        System.out.println("JEMUExSquare= " + JEMUExSquare + " Err " + errJEMUExSquare);
//        System.out.println("JEMUEySquare= " + JEMUEySquare + " Err " + errJEMUEySquare);


        System.out.println("Conventional:\t" + "bJ\t" + (interactionS / temperature) + " Value:\t" + (-dipoleSumSquared / temperature / temperature / numberMolecules)
                + " Err:\t" + (dipoleSumSquaredERR / temperature / temperature / numberMolecules));

        double sumIdeal = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(7);
        double errSumIdeal = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(7);
        System.out.println("IdealMapping:\t" + "bJ\t" + (interactionS / temperature) + " Value:\t" + (sumIdeal / numberMolecules)
                + " Err:\t" + (errSumIdeal / numberMolecules));

        System.out.println("mapping:\t" + "bJ\t" + (interactionS / temperature) + " Value:\t" + (AEE / numberMolecules)
                + " Err:\t" + (AEEER / numberMolecules));

        double sumJEEMJEJE = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(5);
        double errJEEMJEJE = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(5);
        System.out.println("JEEMJEJE:\t" + "bJ\t" + (interactionS / temperature) + " Value:\t" + (sumJEEMJEJE / numberMolecules)
                + " Err:\t" + (errJEEMJEJE / numberMolecules));

        double sumUEE = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.AVERAGE.index).getValue(6);
        double errUEE = ((DataGroup) AEEAccumulator.getData()).getData(AccumulatorAverage.ERROR.index).getValue(6);
        System.out.println("UEE:\t" + "bJ\t" + (interactionS / temperature) + " Value:\t" + (sumUEE / numberMolecules)
                + " Err:\t" + (errUEE / numberMolecules));

        System.out.println("-JEEMJEJE+UEE "+ ((sumJEEMJEJE+sumUEE)/ numberMolecules));


    }

    // ******************* parameters **********************//
    public static class Param extends ParameterBase {
        public boolean mSquare = true;
        public boolean aEE = true;
        public double temperature = 5;// Kelvin
        public double interactionS = 7;
        public double dipoleMagnitude = 8;
        public int steps = 50000;
    }
}
