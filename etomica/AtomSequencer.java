package etomica;

/**
 * AtomSequencer is used to structure all the atoms in a phase into
 * a well defined order.  A single instance of this class is held by
 * the seq field of each atom, and it is the primary point of reference
 * for structuring lists of child atoms in each atom group.  Most of 
 * the iterators that loop through the atoms in the phase use the list
 * order set up using the sequencer.
 *
 * @author David Kofke
 * @version 02.03.09
 */
 
 /* History
  * 10/18/02 (DAK) added sequencerClass method to Factory interface
  */

public interface AtomSequencer extends AtomSequencerFactory {
    
    public interface Factory {
        public AtomLinker makeSequencer(Atom atom);
    }
    
}