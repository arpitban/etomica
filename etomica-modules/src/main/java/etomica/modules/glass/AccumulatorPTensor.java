/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package etomica.modules.glass;

import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.integrator.IntegratorMD;
import etomica.units.dimensions.Null;
import etomica.units.dimensions.Time;

public class AccumulatorPTensor implements IDataSink, IDataSource, DataSourceIndependent {

    protected DataDoubleArray xData;
    protected DataDoubleArray.DataInfoDoubleArray xDataInfo;
    protected DataFunction data;
    protected DataFunction.DataInfoFunction dataInfo;
    protected final DataTag xTag, tag;
    protected double[][] blockSumX;
    protected long[] nBlockSamplesX;
    protected final IntegratorMD integrator;
    protected long nSample = 0;
    protected boolean enabled;
    protected int interval;
    protected double[] sumP2;
    protected int nP, dim;
    protected double volume, temperature, dt;

    public AccumulatorPTensor(IntegratorMD integrator, double dt) {
        this.integrator = integrator;
        this.dt = dt;
        this.nP = integrator.getBox().getSpace().D() == 2 ? 1 : 3;
        tag = new DataTag();
        xTag = new DataTag();
        nBlockSamplesX = new long[60];
        blockSumX = new double[60][nP];
        sumP2 = new double[60];
        volume = integrator.getBox().getBoundary().volume();
        temperature = 1;
        dim = integrator.getBox().getSpace().D();
        reset();
    }

    public void reset() {
        for (int i = 0; i < nBlockSamplesX.length; i++) {
            nBlockSamplesX[i] = 0;
            sumP2[i] = 0;
            for(int k=0; k<nP; k++){
                blockSumX[i][k] = 0;
            }
        }

        data = new DataFunction(new int[]{sumP2.length});
        xData = new DataDoubleArray(new int[]{sumP2.length});
        double[] x = xData.getData();
        for(int i=0; i<xData.getLength(); i++){
            x[i] = dt*(1L<<i);
        }
        xDataInfo = new DataDoubleArray.DataInfoDoubleArray("time", Time.DIMENSION, new int[]{sumP2.length});
        xDataInfo.addTag(xTag);
        dataInfo = new DataFunction.DataInfoFunction("viscosity", Null.DIMENSION, this);
        dataInfo.addTag(tag);
    }


    public void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
        reset();
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public DataTag getTag() {
        return tag;
    }

    @Override
    public void putData(IData inputData) {
        if (!enabled) return;

        double[] x = new double[nP];

        for (int i=0, k = 0; k < dim; k++) {
            for (int l = k+1; l < dim; l++) {
                x[i] = inputData.getValue(k * dim + l);
                i++;
            }
        }

        nSample += 1;
        for (int i = 0; i < blockSumX.length; i++) {
            for(int k=0; k<nP; k++){
                blockSumX[i][k] += x[k];
            }
            if (nSample % (1L << i) == 0) {
                for(int k=0; k<nP; k++){
                    sumP2[i] += blockSumX[i][k]*blockSumX[i][k];
                    blockSumX[i][k] = 0;
                }
                nBlockSamplesX[i]++;
            }
        }
    }


    public IData getData() {
        double[] y = data.getData();
        double fac = dt*volume/(2*nP*temperature);

        for(int i=0; i<sumP2.length; i++){
            y[i] = fac*sumP2[i]/nBlockSamplesX[i]/(1L<<i);
        }
        return data;
    }


    @Override
    public void putDataInfo(IDataInfo inputDataInfo) {
    }

    @Override
    public IDataInfo getDataInfo() {
        return dataInfo;
    }

    @Override
    public DataDoubleArray getIndependentData(int i) {
        return xData;
    }

    @Override
    public DataDoubleArray.DataInfoDoubleArray getIndependentDataInfo(int i) {
        return xDataInfo;
    }

    @Override
    public int getIndependentArrayDimension() {
        return 1;
    }

    @Override
    public DataTag getIndependentTag() {
        return xTag;
    }


    public TemperatureSink makeTemperatureSink() {
        return new TemperatureSink();
    }

    public class TemperatureSink implements IDataSink {
        public void putDataInfo(IDataInfo inputDataInfo) {
        }

        public void putData(IData data) {
            temperature = data.getValue(0);
        }
    }
}