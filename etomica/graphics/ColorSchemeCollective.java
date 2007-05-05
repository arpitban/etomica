package etomica.graphics;
import java.awt.Color;

import etomica.atom.AtomLeafAgentManager;
import etomica.atom.IAtom;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.phase.Phase;

/**
 * Parent class for color schemes that are best implemented by attaching colors
 * to all atoms at once, rather than determining the color of each as it is drawn.
 * The colorAllAtoms method is called by the display if it determines that the
 * ColorScheme is a subclass of this one.
 */
public abstract class ColorSchemeCollective extends ColorScheme implements AgentSource {
    
    protected AtomLeafAgentManager agentManager;
    
    public ColorSchemeCollective(Phase phase) {
        agentManager = new AtomLeafAgentManager(this, phase);
    }
    
    //determine color
    //then assign it to atom like this: atomColors[atomIndex] = color
    public abstract void colorAllAtoms();
    
    public Color getAtomColor(IAtom a) {
        return (Color)agentManager.getAgent(a);
    }
   
    public Class getAgentClass() {
        return Color.class;
    }
    
    public Object makeAgent(IAtom a) {
        return null;
    }
    
    public void releaseAgent(Object agent, IAtom atom) {}
    
}
