/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.osmoticvirial;

import etomica.integrator.Integrator;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorManagerMC;
import etomica.integrator.mcmove.MCMoveMoleculeExchange;
import etomica.integrator.mcmove.MCMoveVolumeExchange;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.space.Space;
import etomica.util.random.IRandom;

/**
 * Simple Gibbs-ensemble Monte Carlo integrator. Used to evaluate fluid-fluid
 * box coexistence. Written to apply to only two boxs.
 * 
 * @author David Kofke
 */
public class IntegratorRGEMC extends IntegratorManagerMC {

    private MCMoveGeometricClusterRestrictedGE mcMoveGeometricClusterRestrictedGE;
    private Space space;


    public IntegratorRGEMC(IRandom random, Space space) {
        super(random);
        this.space = space;
    }

    public void addIntegrator(Integrator newIntegrator) {
        if (!(newIntegrator instanceof IntegratorBox)) {
            throw new IllegalArgumentException("Sub integrators must be able to handle a box");
        }
        if (nIntegrators == 2) {
            throw new IllegalArgumentException("Only 2 sub-integrators can be added");
        }
        super.addIntegrator(newIntegrator);
        if (nIntegrators == 2) {

            mcMoveGeometricClusterRestrictedGE =
                    new MCMoveGeometricClusterRestrictedGE((PotentialMasterCell) ((IntegratorBox)newIntegrator).getPotentialMaster(),
                    space, random, 1.5, ((IntegratorBox)integrators[0]).getBox(),((IntegratorBox)integrators[1]).getBox());
            moveManager.recomputeMoveFrequencies();
            moveManager.addMCMove(mcMoveGeometricClusterRestrictedGE);
        }
    }

    /**
     * Returns the object that performs the molecule-exchange move in the GE
     * simulation. Having handle to this object is needed to adjust trial
     * frequency and view acceptance rate.
     */
    public MCMoveGeometricClusterRestrictedGE getMCMoveMoleculeExchange() {
        return mcMoveGeometricClusterRestrictedGE;
    }

}