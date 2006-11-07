package etomica.normalmode;

import etomica.atom.Atom;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.data.DataSourceScalar;
import etomica.data.meter.Meter;
import etomica.phase.Phase;
import etomica.space.Vector;
import etomica.units.Energy;

/**
 * Meter that calculates the harmonic energy of a configuration given
 * eigenvectors and omegas corresponding to wave vectors.
 * @author Andrew Schultz
 */
public class MeterHarmonicEnergy extends DataSourceScalar implements Meter {

    public MeterHarmonicEnergy() {
        super("Harmonic Energy", Energy.DIMENSION);
        iterator = new AtomIteratorAllMolecules();
    }
    
    public void setNormalCoordWrapper(NormalCoordMapper newNormalCoordWrapper) {
        normalCoordMapper = newNormalCoordWrapper;
    }
    
    public NormalCoordMapper getNormalCoordWrapper() {
        return normalCoordMapper;
    }

    public double getDataAsScalar() {
        double energySum = 0;
        for (int iVector = 0; iVector < waveVectors.length; iVector++) {
            for (int i=0; i<normalDim; i++) {
                realT[i] = 0;
                imaginaryT[i] = 0;
            }
            iterator.reset();
            int atomCount = 0;
            // sum T over atoms
            while (iterator.hasNext()) {
                Atom atom = iterator.nextAtom();
                normalCoordMapper.calcU(atom, atomCount, u);
                double kR = waveVectors[iVector].dot(latticePositions[atomCount]);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for (int i=0; i<normalDim; i++) {
                    realT[i] += coskR * u[i];
                    imaginaryT[i] += sinkR * u[i];
                }
                
                atomCount++;
            }
            
            // we want to calculate Q = A T
            // where A is made up of eigenvectors as columns
            for (int i=0; i<normalDim; i++) {
                double realCoord = 0, imaginaryCoord = 0;
                for (int j=0; j<normalDim; j++) {
                    realCoord += realT[j] * eigenVectors[iVector][j][i];
                    imaginaryCoord += imaginaryT[j] * eigenVectors[iVector][j][i];
                }
                // we were supposed to divide T by sqrt(atomCount), but it's easier to handle that here
                double normalCoord = (realCoord*realCoord + imaginaryCoord*imaginaryCoord)/atomCount;
                energySum += normalCoord * omegaSquared[iVector][i];
            }
        }
        return 0.5*energySum;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase newPhase) {
        phase = newPhase;
        iterator.setPhase(phase);
        normalDim = normalCoordMapper.getNormalDim();

        latticePositions = new Vector[phase.getSpeciesMaster().moleculeCount()];

        iterator.reset();
        int atomCount = 0;
        while (iterator.hasNext()) {
            latticePositions[atomCount] = phase.space().makeVector();
            Atom atom = iterator.nextAtom();
            Vector atomPos = atom.type.getPositionDefinition().position(atom);
            latticePositions[atomCount].E(atomPos);
            atomCount++;
        }

        normalCoordMapper.setNumAtoms(iterator.size());
        u = new double[normalDim];
        realT = new double[normalDim];
        imaginaryT = new double[normalDim];
        
        // notifies NormalCoordWrapper of the nominal position of each atom
        iterator.reset();
        atomCount = 0;
        while (iterator.hasNext()) {
            Atom atom = iterator.nextAtom();
            normalCoordMapper.initNominalU(atom, atomCount);
            atomCount++;
        }
    }
    
    public void setWaveVectors(Vector[] newWaveVectors) {
        waveVectors = newWaveVectors;
    }
    
    public void setEigenvectors(double[][][] newEigenVectors) {
        eigenVectors = newEigenVectors;
    }
    
    public void setOmegaSquared(double[][] newOmegaSquared) {
        omegaSquared = newOmegaSquared;
    }
    
    private static final long serialVersionUID = 1L;
    protected NormalCoordMapper normalCoordMapper;
    protected Vector[] latticePositions;
    protected final AtomIteratorAllMolecules iterator;
    protected Phase phase;
    protected int normalDim;
    protected double[] u;
    protected double[] realT, imaginaryT;
    protected Vector[] waveVectors;
    protected double[][][] eigenVectors;
    protected double[][] omegaSquared;
}
