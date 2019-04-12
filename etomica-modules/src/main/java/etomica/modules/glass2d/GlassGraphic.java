/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.glass2d;

import etomica.action.IAction;
import etomica.atom.DiameterHashByType;
import etomica.data.*;
import etomica.data.meter.*;
import etomica.data.types.DataDouble;
import etomica.data.types.DataTensor;
import etomica.graphics.*;
import etomica.integrator.IntegratorListenerAction;
import etomica.modifier.ModifierBoolean;
import etomica.util.ParseArgs;

import java.awt.*;
import java.util.ArrayList;

public class GlassGraphic extends SimulationGraphic {

    private final static String APP_NAME = "Lennard-Jones Molecular Dynamics";
    private final static int REPAINT_INTERVAL = 20;
    protected SimGlass sim;

    public GlassGraphic(final SimGlass simulation) {

        super(simulation, TABBED_PANE, APP_NAME, REPAINT_INTERVAL);

        ArrayList<DataPump> dataStreamPumps = getController().getDataStreamPumps();

        this.sim = simulation;

        sim.activityIntegrate.setSleepPeriod(1);

        //display of box, timer
        ColorSchemeByType colorScheme = new ColorSchemeByType();
        colorScheme.setColor(sim.speciesA.getLeafType(), Color.red);
        colorScheme.setColor(sim.speciesB.getLeafType(), Color.blue);
        getDisplayBox(sim.box).setColorScheme(colorScheme);
        DiameterHashByType diameterHash = (DiameterHashByType) getDisplayBox(sim.box).getDiameterHash();
        diameterHash.setDiameter(sim.speciesB.getLeafType(), sim.isLJ ? 0.88 : 1 / 1.4);
        diameterHash.setDiameter(sim.speciesA.getLeafType(), 1);
//        sim.integrator.addListener(new IntervalActionAdapter(this.getDisplayBoxPaintAction(sim.box)));

        //meters and displays
        final MeterRDF rdfMeter = new MeterRDF(sim.getSpace());
        IntegratorListenerAction rdfMeterListener = new IntegratorListenerAction(rdfMeter);
        sim.integrator.getEventManager().addListener(rdfMeterListener);
        rdfMeterListener.setInterval(10);
        rdfMeter.getXDataSource().setXMax(4.0);
        rdfMeter.setBox(sim.box);
        DisplayPlot rdfPlot = new DisplayPlot();
        DataPump rdfPump = new DataPump(rdfMeter, rdfPlot.getDataSet().makeDataSink());
        IntegratorListenerAction rdfPumpListener = new IntegratorListenerAction(rdfPump);
        sim.integrator.getEventManager().addListener(rdfPumpListener);
        rdfPumpListener.setInterval(10);
        dataStreamPumps.add(rdfPump);

        rdfPlot.setDoLegend(false);
        rdfPlot.getPlot().setTitle("Radial Distribution Function");
        rdfPlot.setLabel("RDF");

        DataSourceCountTime timeCounter = new DataSourceCountTime(sim.integrator);

        DisplayBox dbox = new DisplayBox(sim, sim.box);
        dbox.setLabel("Displacement");
        DisplayBoxCanvas2DGlass canvas = new DisplayBoxCanvas2DGlass(dbox, sim.getSpace(), sim.getController());
        dbox.setBoxCanvas(canvas);
        add(dbox);
        dbox.setColorScheme(colorScheme);
        dbox.setDiameterHash(diameterHash);
        canvas.setVisible(false);
        canvas.setVisible(true);

        DisplayBox dbox2 = new DisplayBox(sim, sim.box);
        dbox2.setLabel("Colors");
        add(dbox2);
        ColorSchemeDeviation colorSchemeDeviation = new ColorSchemeDeviation(sim.box);
        dbox2.setColorScheme(colorSchemeDeviation);
        dbox2.setDiameterHash(diameterHash);
        dbox2.canvas.setVisible(false);
        dbox2.canvas.setVisible(true);

        DeviceCheckBox swapCheckbox = new DeviceCheckBox("isothermal", new ModifierBoolean() {
            @Override
            public void setBoolean(boolean b) {

                if (sim.integrator.isIsothermal() == b) return;
                if (b) {
                    sim.integrator.setIsothermal(true);
                    sim.integrator.setIntegratorMC(sim.integratorMC, 10000);
                } else {
                    sim.integrator.setIntegratorMC(null, 0);
                    sim.integrator.setIsothermal(false);
                    canvas.reset();
                    colorSchemeDeviation.reset();
                }
            }

            @Override
            public boolean getBoolean() {
                return sim.integrator.isIsothermal();
            }
        });
        swapCheckbox.setController(sim.getController());
        add(swapCheckbox);


        IAction repaintAction = new IAction() {
            public void actionPerformed() {
                if (sim.integrator.isIsothermal()) return;
                dbox.repaint();
                dbox2.repaint();
            }
        };
        IntegratorListenerAction repaintAction2 = new IntegratorListenerAction(repaintAction);
        repaintAction2.setInterval(100);
        sim.integrator.getEventManager().addListener(repaintAction2);

        final IAction resetDataAction = new IAction() {
            public void actionPerformed() {
                getController().getSimRestart().getDataResetAction().actionPerformed();
                rdfMeter.reset();
            }
        };


        //add meter and display for current kinetic temperature

        MeterTemperature thermometer = new MeterTemperature(sim.box, space.D());
        DataFork temperatureFork = new DataFork();
        final DataPump temperaturePump = new DataPump(thermometer, temperatureFork);
        IntegratorListenerAction temperaturePumpListener = new IntegratorListenerAction(temperaturePump);
        sim.integrator.getEventManager().addListener(temperaturePumpListener);
        temperaturePumpListener.setInterval(10);
        final AccumulatorHistory temperatureHistory = new AccumulatorHistory();
        temperatureHistory.setTimeDataSource(timeCounter);
        temperatureFork.setDataSinks(new IDataSink[]{temperatureHistory});

        dataStreamPumps.add(temperaturePump);

        // Number density box
        MeterDensity densityMeter = new MeterDensity(sim.getSpace());
        densityMeter.setBox(sim.box);
        final DisplayTextBox densityBox = new DisplayTextBox();
        final DataPump densityPump = new DataPump(densityMeter, densityBox);
        IntegratorListenerAction densityPumpListener = new IntegratorListenerAction(densityPump);
        sim.integrator.getEventManager().addListener(densityPumpListener);
        densityPumpListener.setInterval(10);
        dataStreamPumps.add(densityPump);
        densityBox.setLabel("Number Density");

        MeterEnergy eMeter = new MeterEnergy(sim.integrator.getPotentialMaster(), sim.box);
        AccumulatorHistory energyHistory = new AccumulatorHistory();
        energyHistory.setTimeDataSource(timeCounter);
        DataPump energyPump = new DataPump(eMeter, energyHistory);
        IntegratorListenerAction energyPumpListener = new IntegratorListenerAction(energyPump);
        sim.integrator.getEventManager().addListener(energyPumpListener);
        energyPumpListener.setInterval(60);
        energyHistory.setPushInterval(5);
        dataStreamPumps.add(energyPump);

        MeterPotentialEnergy peMeter = new MeterPotentialEnergy(sim.integrator.getPotentialMaster(), sim.box);
        AccumulatorHistory peHistory = new AccumulatorHistory();
        peHistory.setTimeDataSource(timeCounter);
        final AccumulatorAverageCollapsing peAccumulator = new AccumulatorAverageCollapsing();
        peAccumulator.setPushInterval(10);
        DataFork peFork = new DataFork(new IDataSink[]{peHistory, peAccumulator});
        DataPump pePump = new DataPump(peMeter, peFork);
        IntegratorListenerAction pePumpListener = new IntegratorListenerAction(pePump);
        sim.integrator.getEventManager().addListener(pePumpListener);
        pePumpListener.setInterval(60);
        peHistory.setPushInterval(5);
        dataStreamPumps.add(pePump);

        MeterKineticEnergy keMeter = new MeterKineticEnergy(sim.box);
        AccumulatorHistory keHistory = new AccumulatorHistory();
        keHistory.setTimeDataSource(timeCounter);
        DataFork keFork = new DataFork();
        DataPump kePump = new DataPump(keMeter, keFork);
        keFork.addDataSink(keHistory);
        final AccumulatorAverage keAvg = new AccumulatorAverageCollapsing();
        keFork.addDataSink(keAvg);
        IntegratorListenerAction kePumpListener = new IntegratorListenerAction(kePump);
        sim.integrator.getEventManager().addListener(kePumpListener);
        kePumpListener.setInterval(60);
        keHistory.setPushInterval(5);
        dataStreamPumps.add(kePump);

        DisplayPlot ePlot = new DisplayPlot();
        energyHistory.setDataSink(ePlot.getDataSet().makeDataSink());
        ePlot.setLegend(new DataTag[]{energyHistory.getTag()}, "Total");
        peHistory.setDataSink(ePlot.getDataSet().makeDataSink());
        ePlot.setLegend(new DataTag[]{peHistory.getTag()}, "Potential");
        keHistory.setDataSink(ePlot.getDataSet().makeDataSink());
        ePlot.setLegend(new DataTag[]{keHistory.getTag()}, "Kinetic");

        ePlot.getPlot().setTitle("Energy History");
        ePlot.setDoLegend(true);
        ePlot.setLabel("Energy");

        MeterPressureTensorFromIntegrator pMeter = new MeterPressureTensorFromIntegrator(space);
        pMeter.setIntegrator(sim.integrator);
        final AccumulatorAverageCollapsing pAccumulator = new AccumulatorAverageCollapsing();
        DataProcessorTensorTrace tracer = new DataProcessorTensorTrace();
        final DataPump pPump = new DataPump(pMeter, tracer);
        tracer.setDataSink(pAccumulator);
        sim.integrator.getEventManager().addListener(new IntegratorListenerAction(pPump));
        pAccumulator.setPushInterval(10);
        dataStreamPumps.add(pPump);

        final DisplayTextBoxesCAE pDisplay = new DisplayTextBoxesCAE();
        pDisplay.setAccumulator(pAccumulator);
        final DisplayTextBoxesCAE peDisplay = new DisplayTextBoxesCAE();
        peDisplay.setAccumulator(peAccumulator);

        //************* Lay out components ****************//

        GridBagConstraints vertGBC = SimulationPanel.getVertGBC();

        getDisplayBox(sim.box).setScale(0.7);

        //temperature selector
        DeviceSlider temperatureSelect = new DeviceSlider(sim.getController(), sim.integrator, "temperature");
        temperatureSelect.setPrecision(2);
        temperatureSelect.setNMajor(4);
        temperatureSelect.setMinimum(0.0);
        temperatureSelect.setMaximum(2.0);
        temperatureSelect.setLabel("Temperature");
        temperatureSelect.setShowBorder(true);
        temperatureSelect.setShowValues(true);
        temperatureSelect.setEditValues(true);

        temperatureSelect.setPostAction(new IAction() {
            public void actionPerformed() {
                resetDataAction.actionPerformed();
                getDisplayBox(sim.box).repaint();
            }
        });
        canvas.reset();
        dbox.repaint();
        colorSchemeDeviation.reset();
        dbox2.repaint();

        IAction resetAction = new IAction() {
            public void actionPerformed() {
                rdfMeter.reset();

                // Reset density (Density is set and won't change, but
                // do this anyway)
                densityPump.actionPerformed();
                densityBox.repaint();

                temperaturePump.actionPerformed();

                // IS THIS WORKING?
                pPump.actionPerformed();
                pDisplay.putData(pAccumulator.getData());
                pDisplay.repaint();
                peDisplay.putData(peAccumulator.getData());
                peDisplay.repaint();

                getDisplayBox(sim.box).graphic().repaint();
            }
        };

        this.getController().getReinitButton().setPostAction(resetAction);
        this.getController().getResetAveragesButton().setPostAction(resetAction);

        getPanel().controlPanel.add(temperatureSelect.graphic(), vertGBC);

        add(rdfPlot);
        add(ePlot);
        add(densityBox);
        add(pDisplay);
        add(peDisplay);

    }

    public static void main(String[] args) {
        SimGlass.GlassParams params = new SimGlass.GlassParams();
        if (args.length > 0) {
            ParseArgs.doParseArgs(params, args);
        } else {
            params.doSwap = true;
            params.doLJ = false;
            params.nA = params.nB = 400;
            params.density = 1.35;
        }
        SimGlass sim = new SimGlass(params.D, params.nA, params.nB, params.density, params.doSwap, params.doLJ);

        GlassGraphic ljmdGraphic = new GlassGraphic(sim);
        SimulationGraphic.makeAndDisplayFrame
                (ljmdGraphic.getPanel(), APP_NAME);
    }

    /**
     * Inner class to find the total pressure of the system from the pressure
     * tensor.
     */
    public static class DataProcessorTensorTrace extends DataProcessor {

        public DataProcessorTensorTrace() {
            data = new DataDouble();
        }

        protected IData processData(IData inputData) {
            // take the trace and divide by the dimensionality
            data.x = ((DataTensor) inputData).x.trace() / ((DataTensor) inputData).x.D();
            return data;
        }

        protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
            if (!(inputDataInfo instanceof DataTensor.DataInfoTensor)) {
                throw new IllegalArgumentException("Gotta be a DataInfoTensor");
            }
            dataInfo = new DataDouble.DataInfoDouble(inputDataInfo.getLabel(), inputDataInfo.getDimension());
            return dataInfo;
        }

        protected final DataDouble data;
    }

}


