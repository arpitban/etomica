package etomica.action;

/**
 * Switches controller between paused and unpaused conditions, or starts
 * it if not yet active.
 */
public class ControllerToggle extends ControllerActionAdapter {
        
    public ControllerToggle() {
        super("Start/pause/resume");
    }
    
    public void actionPerformed() {
    	if(controller == null) return;
        if(!controller.isActive()) controller.start();
        else if(!controller.isPaused()) controller.pause();
        else controller.unPause();
    }

}//end of ControllerToggle
    