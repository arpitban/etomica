package etomica.rotation;

import java.awt.Color;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IAtomTypeSphere;
import etomica.api.ISpecies;
import etomica.atom.iterator.ApiBuilder;
import etomica.box.Box;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.DataSourceCountTime;
import etomica.data.meter.MeterEnergy;
import etomica.data.meter.MeterKineticEnergyFromIntegrator;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorVelocityVerletRattle;
import etomica.listener.IntegratorListenerAction;
import etomica.models.water.ConformationWater3P;
import etomica.models.water.P2WaterSPC;
import etomica.models.water.SpeciesWater3P;
import etomica.potential.P2Electrostatic;
import etomica.potential.P2LennardJones;
import etomica.potential.PotentialGroup;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularNonperiodic;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.Electron;
import etomica.units.Kelvin;
import etomica.util.Constants;
import etomica.util.HistoryCollapsingAverage;

public class WaterTrimerRattle {

    public static SimulationGraphic makeWaterDroplet() {
        Space space = Space3D.getInstance();
        Simulation sim = new Simulation(space);
        Box box = new Box(new BoundaryRectangularNonperiodic(sim.getSpace()), space);
        sim.addBox(box);
        SpeciesWater3P species = new SpeciesWater3P(sim.getSpace(), true);
        sim.addSpecies(species);
        box.setNMolecules(species, 3);
        box.setDensity(0.9/18.0*Constants.AVOGADRO/1E24);
        ConfigurationWater3_3P config = new ConfigurationWater3_3P();
//        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicFcc(), space);
        config.initializeCoordinates(box);
        box.getBoundary().setBoxSize(space.makeVector(new double[]{15,15,15}));
        PotentialMaster potentialMaster = new PotentialMaster();
        double timeInterval = 0.001;
        int maxIterations = 100;
        IntegratorVelocityVerletRattle integrator = new IntegratorVelocityVerletRattle(sim, potentialMaster, space);
        double lOH = ConformationWater3P.bondLengthOH;
        double lHH = Math.sqrt(2*lOH*lOH*(1-Math.cos(ConformationWater3P.angleHOH)));
        integrator.setBondConstraints(species, new int[][]{{0,2},{1,2},{0,1}}, new double[]{lOH, lOH, lHH});
        integrator.setTimeStep(timeInterval);
        integrator.printInterval = 1000;
        integrator.setMaxIterations(maxIterations);
        integrator.setBox(box);
//        integrator.setIsothermal(true);
        integrator.setTemperature(Kelvin.UNIT.toSim(0));
        integrator.setThermostatInterval(100);
        ActivityIntegrate ai = new ActivityIntegrate(integrator);
        sim.getController().addAction(ai);
//        System.out.println("h1 at "+((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition());
//        System.out.println("o at "+((IAtomPositioned)box.getLeafList().getAtom(2)).getPosition());

        double chargeOxygen = Electron.UNIT.toSim(-0.82);
        double chargeHydrogen = Electron.UNIT.toSim(0.41);
        
        IAtomTypeSphere oType = species.getOxygenType();
        IAtomTypeSphere hType = species.getHydrogenType();
        double epsOxygen = new P2WaterSPC(space).getEpsilon();
        double sigOxygen = new P2WaterSPC(space).getSigma();
        PotentialGroup pGroup = potentialMaster.makePotentialGroup(2);
        P2LennardJones potentialLJOO = new P2LennardJones(space, sigOxygen, epsOxygen);
        pGroup.addPotential(potentialLJOO, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{oType,oType}));

        P2Electrostatic potentialQHH = new P2Electrostatic(space);
        potentialQHH.setCharge1(chargeHydrogen);
        potentialQHH.setCharge2(chargeHydrogen);
        pGroup.addPotential(potentialQHH, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{hType,hType}));

        P2Electrostatic potentialQOO = new P2Electrostatic(space);
        potentialQOO.setCharge1(chargeOxygen);
        potentialQOO.setCharge2(chargeOxygen);
        pGroup.addPotential(potentialQOO, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{oType,oType}));
        
        P2Electrostatic potentialQOH = new P2Electrostatic(space);
        potentialQOH.setCharge1(chargeOxygen);
        potentialQOH.setCharge2(chargeHydrogen);
        pGroup.addPotential(potentialQOH, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{oType,hType}));
        pGroup.addPotential(potentialQOH, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{hType,oType}));

        potentialMaster.addPotential(pGroup, new ISpecies[]{species,species});
        if (false) {
            ai.setSleepPeriod(2);
            SimulationGraphic graphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, "Rigid", 1, space, sim.getController());
            ((ColorSchemeByType)graphic.getDisplayBox(box).getColorScheme()).setColor(species.getHydrogenType(), Color.WHITE);
            ((ColorSchemeByType)graphic.getDisplayBox(box).getColorScheme()).setColor(species.getOxygenType(), Color.RED);
    
            MeterEnergy meterE = new MeterEnergy(potentialMaster, box);
            meterE.setKinetic(new MeterKineticEnergyFromIntegrator(integrator));
            meterE.setPotential(new MeterPotentialEnergyFromIntegrator(integrator));
            AccumulatorHistory history = new AccumulatorHistory(new HistoryCollapsingAverage());
            history.setTimeDataSource(new DataSourceCountTime(integrator));
            DataPump pump = new DataPump(meterE, history);
            DisplayPlot ePlot = new DisplayPlot();
            history.setDataSink(ePlot.getDataSet().makeDataSink());
            IntegratorListenerAction pumpListener = new IntegratorListenerAction(pump);
            pumpListener.setInterval(10);
            integrator.getEventManager().addListener(pumpListener);
            ePlot.setLabel("Energy");
            graphic.add(ePlot);
            return graphic;
        }
        sim.getController().actionPerformed();
        return null;
    }

    public static void main(String[] args) {
        SimulationGraphic graphic = makeWaterDroplet();
        if (graphic != null) {
            graphic.makeAndDisplayFrame();
        }
    }
    
    public static class Applet extends javax.swing.JApplet {

        public void init() {
            SimulationGraphic graphic = makeWaterDroplet();

            getContentPane().add(graphic.getPanel());
        }

        private static final long serialVersionUID = 1L;
    }
}
