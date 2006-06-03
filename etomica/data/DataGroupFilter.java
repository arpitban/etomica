package etomica.data;

import etomica.data.types.CastToGroup;
import etomica.data.types.DataGroup;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.util.Arrays;


/**
 * Extracts one or more of the Data instances wrapped in a DataGroup.  If a single instance,
 * output is the instance itself; if multiple instances, a new DataGroup with the subset is
 * output.
 *
 * @author David Kofke
 *
 */

public class DataGroupFilter extends DataProcessor {

    /**
     * Creates a filter that will take the DataGroup element indicated
     * by the index, counting from 0.
     */
    public DataGroupFilter(int index) {
        this(new int[] {index});
    }

    public DataTag getTag() {
        // we have no tag
        return null;
    }

    /**
     * Creates a filter that will take the set of elements from the DataGroup
     * corresponding to the given index values (with indexes counting from 0).
     */
    public DataGroupFilter(int[] indexes) {
        this.indexes = (int[])indexes.clone();
        singleInstance = (indexes.length == 1); 
    }

    /**
     * Returns the output data that was established with a previous call to 
     * processDataInfo.
     */
    public Data processData(Data data) {
        if (outputData == null) {
            if(singleInstance) {
                if(indexes[0] < ((DataGroup)data).getNData()) {
                    outputData = ((DataGroup)data).getData(indexes[0]);
                } else {
                    throw new ArrayIndexOutOfBoundsException("DataFilter was constructed to extract a Data element with an index that is larger than the number of Data elements wrapped in the DataGroup. Number of elements: "+((DataGroup)data).getNData()+"; index array: "+Arrays.toString(indexes));
                }
            } else {
                Data[] pushedData = new Data[indexes.length];
                try {
                    for (int i=0; i<indexes.length; i++) {
                        pushedData[i] = ((DataGroup)data).getData(indexes[i]);
                    }
                } catch(ArrayIndexOutOfBoundsException ex) {
                    throw new ArrayIndexOutOfBoundsException("DataFilter was constructed to extract a Data element with an index that is larger than the number of Data elements wrapped in the DataGroup. Number of elements: "+((DataGroup)data).getNData()+"; index array: "+Arrays.toString(indexes));
                }
                outputData = new DataGroup(pushedData);
            }
        }
        return outputData;
    }
    
    
    /**
     * Determines data that will be output, using the given DataInfo and
     * the index specification given at construction.  Returns the DataInfo
     * of the data that will be output.
     */
    protected DataInfo processDataInfo(DataInfo inputDataInfo) {
        outputData = null;
        int nData = ((DataInfoGroup)inputDataInfo).getNDataInfo();
        if(singleInstance) {
            if(indexes[0] < nData) {
                return ((DataInfoGroup)inputDataInfo).getSubDataInfo(indexes[0]);
            }
            throw new ArrayIndexOutOfBoundsException("DataFilter was constructed to extract a Data element with an index that is larger than the number of Data elements wrapped in the DataGroup. Number of elements: "+nData+"; index array: "+Arrays.toString(indexes));
        }
        DataInfo[] pushedDataInfo = new DataInfo[indexes.length];
        try {
            for (int i=0; i<indexes.length; i++) {
                pushedDataInfo[i] = ((DataInfoGroup)inputDataInfo).getSubDataInfo(indexes[i]);
            }
        } catch(ArrayIndexOutOfBoundsException ex) {
            throw new ArrayIndexOutOfBoundsException("DataFilter was constructed to extract a Data element with an index that is larger than the number of Data elements wrapped in the DataGroup. Number of elements: "+nData+"; index array: "+Arrays.toString(indexes));
        }
        return new DataInfoGroup(inputDataInfo.getLabel(), inputDataInfo.getDimension(), pushedDataInfo);
    }

    /**
     * Returns null if the given DataInfo is for a DataGroup; otherwise
     * throws an exception.
     */
    public DataProcessor getDataCaster(DataInfo incomingDataInfo) {
        if(!(incomingDataInfo instanceof DataInfoGroup)) {
            throw new IllegalArgumentException("DataGroupFilter must operate on a DataGroup");
        }
        return null;
    }
    
    private Data outputData;
    private final boolean singleInstance;
    private final int[] indexes;
}
