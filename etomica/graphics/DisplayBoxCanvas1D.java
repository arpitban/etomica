package etomica.graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.TextField;
import java.util.Iterator;

import etomica.action.activity.Controller;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBoundary;
import etomica.api.IVector;
import etomica.atom.AtomFilter;
import etomica.atom.AtomFilterCollective;
import etomica.atom.AtomTypeWell;
import etomica.space.Boundary;
import etomica.space.ISpace;

    /* History of changes
     * 7/16/02 (DAK) Modified for AtomType.Sphere diameter and radius method to take atom as argument.
     * 09/07/02 (DAK) added atomFilter
     */

//Class used to define canvas onto which configuration is drawn
public class DisplayBoxCanvas1D extends DisplayCanvas {
    private TextField scaleText = new TextField();
    private Font font = new Font("sansserif", Font.PLAIN, 10);
    //  private int annotationHeight = font.getFontMetrics().getHeight();
    private int annotationHeight = 12;
    private int[] shiftOrigin = new int[2];     //work vector for drawing overflow images
    private final static Color wellColor = new Color(185,185,185, 110);
    private final ISpace space;
    
    public DisplayBoxCanvas1D(ISpace _space, DisplayBox _box, Controller _controller) {
        super(_controller);
        displayBox = _box;
        space = _space;
        scaleText.setVisible(true);
        scaleText.setEditable(false);
        scaleText.setBounds(0,0,100,50);
    }
        
    /**
    * Sets the size of the display to a new value and scales the image so that
    * the box fits in the canvas in the same proportion as before.
    */
    public void scaleSetSize(int width, int height) {
        if(getBounds().width * getBounds().height != 0) {  //reset scale based on larger size change
            double ratio1 = (double)width/(double)getBounds().width;
            double ratio2 = (double)height/(double)getBounds().height;
            double factor = Math.min(ratio1, ratio2);
    //        double factor = (Math.abs(Math.log(ratio1)) > Math.abs(Math.log(ratio2))) ? ratio1 : ratio2;
            displayBox.setScale(displayBox.getScale()*factor);
            setSize(width, height);
        }
    }
            
          
    //Override superclass methods for changing size so that scale is reset with any size change  
    // this setBounds is ultimately called by all other setSize, setBounds methods
    public void setBounds(int x, int y, int width, int height) {
        if(width <= 0 || height <= 0) return;
        super.setBounds(x,y,width,height);
        createOffScreen(width,height);
    }
       
    private void drawAtom(Graphics g, int origin[], IAtomPositioned a) {
        
        IVector r = a.getPosition();
        boolean drawWell = false;
        int sigmaP, xP, yP, baseXP, baseYP;

        if(a.getType() instanceof AtomTypeWell) {
            drawWell = true;
        }

        g.setColor(displayBox.getColorScheme().getAtomColor(a));
            
        baseXP = origin[0] + (int)(displayBox.getToPixels()*r.x(0));
        int drawingHeight = displayBox.getDrawingHeight();
        baseYP = origin[1] + drawingHeight/2;
        if(a.getType() instanceof IAtomTypeSphere) {
            /* Draw the core of the atom */
            sigmaP = (int)(displayBox.getToPixels()*((IAtomTypeSphere)a.getType()).getDiameter());
            if (sigmaP == 0) {
                sigmaP = 1;
            }
            xP = baseXP - (sigmaP>>1);
            yP = baseYP - (drawingHeight >> 1);
            g.fillRect(xP, yP, sigmaP, drawingHeight);
            /* Draw the surrounding well, if any */
            if(drawWell) {
                sigmaP = (int)(displayBox.getToPixels()*((AtomTypeWell)a.getType()).wellDiameter());
                xP = baseXP - (sigmaP>>1);
                g.setColor(wellColor);
                g.drawRect(xP, yP, sigmaP, drawingHeight);
            }
//            a.type.electroType().draw(g, origin, displayBox.getToPixels(), r);
        } else { // Not a sphere, wall, or one of their derivatives...
            // Do nothing (how do you draw an object of unkown shape?)
        }
    }
            
    protected boolean computeShiftOrigin(IAtomPositioned a, IBoundary b) {
        if(a.getType() instanceof IAtomTypeSphere) {
            OverflowShift overflow = new OverflowShift(space);
            float[][] shifts = overflow.getShifts(b, a.getPosition(),0.5*((IAtomTypeSphere)a.getType()).getDiameter());
            for(int i=0; i<shifts.length; i++) {
                shiftOrigin[0] = displayBox.getOrigin()[0] + (int)(displayBox.getToPixels()*shifts[i][0]);
                shiftOrigin[1] = displayBox.getOrigin()[1] + (int)(displayBox.getToPixels()*shifts[i][1]);
            }
            return(true);
        }
        return(false);
    }
      
    /**
    * doPaint is the method that handles the drawing of the box to the screen.
    * Several variables and conditions affect how the image is drawn.  First,
    * the Unit.Length.Sim class variable <code>TO_PIXELS</code> performs the conversion between simulation
    * length units (Angstroms) and pixels.  The default value is 10 pixels/Angstrom
    * reflecting the default size of the box (300 pixels by 300 pixels) and the
    * default box size (30 by 30 A).  
    * The field <code>scale</code> is a multiplicative factor that directly
    * scales up or down the size of the image; this value is adjusted automatically
    * whenever shells of periodic images are drawn, to permit the central image and all 
    * of the specified periodic images to fit in the drawing of the box.  
    *
    * @param g The graphic object to which the image of the box is drawn
    */
    public void doPaint(Graphics g) {
        if(!isVisible() || displayBox.getBox() == null) {return;}
        int w = getSize().width;
        int h = getSize().height;
            
        g.setColor(getBackground());
        g.fillRect(0,0,w,h);
        displayBox.computeImageParameters2(w, h);

        //Draw other features if indicated
//        if(drawBoundary>DRAW_BOUNDARY_NONE) {displayBox.getBox().boundary().draw(g, displayBox.getOrigin(), displayBox.getScale());}
//        if(displayBox.getDrawBoundary()) {displayBox.getBox().boundary().draw(g, displayBox.getOrigin(), displayBox.getScale());}

        //do drawing of all drawing objects that have been added to the display
        for(Iterator iter=displayBox.getDrawables().iterator(); iter.hasNext(); ) {
            Drawable obj = (Drawable)iter.next();
            obj.draw(g, displayBox.getOrigin(), displayBox.getToPixels());
        }
            
        //Draw all atoms
        if(displayBox.getColorScheme() instanceof ColorSchemeCollective) {
            ((ColorSchemeCollective)displayBox.getColorScheme()).colorAllAtoms();
        }
        IAtomSet leafList = displayBox.getBox().getLeafList();
        int nLeaf = leafList.getAtomCount();
        AtomFilter atomFilter = displayBox.getAtomFilter();
        if (atomFilter instanceof AtomFilterCollective) {
            ((AtomFilterCollective)atomFilter).resetFilter();
        }
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            IAtomPositioned a = (IAtomPositioned)leafList.getAtom(iLeaf);
            if(atomFilter != null && atomFilter.accept(a)) continue;
            drawAtom(g, displayBox.getOrigin(), a);
        }
            
        //Draw overflow images if so indicated
        //This needs some work to make more general
///        for(Atom a=displayBox.getBox().firstAtom(); a!=null; a=a.nextAtom()) {
///            if(computeShiftOrigin(a, boundary)) {
///                drawAtom(g, shiftOrigin, a);
///            }
///        }

        //Draw periodic images if indicated ONLY for an etomica Boundary
        if(displayBox.getBox().getBoundary() instanceof Boundary) {
	        if(displayBox.getImageShells() > 0) {
	            double[][] origins = ((Boundary)displayBox.getBox().getBoundary()).imageOrigins(displayBox.getImageShells());  //more efficient to save rather than recompute each time
	            for(int i=0; i<origins.length; i++) {
	                g.copyArea(displayBox.getOrigin()[0],displayBox.getOrigin()[1],displayBox.getDrawSize()[0],displayBox.getDrawSize()[1],(int)(displayBox.getToPixels()*origins[i][0]),(int)(displayBox.getToPixels()*origins[i][1]));
	            }
	        }
        }
        //Draw bar showing scale if indicated
        if(writeScale) {
            g.setColor(Color.lightGray);
            g.fillRect(0,getSize().height-annotationHeight,getSize().width,annotationHeight);
            g.setColor(Color.black);
            g.setFont(font);
            g.drawString("Scale: "+Integer.toString((int)(100*displayBox.getScale()))+"%", 0, getSize().height-3);
        }
    }
}  //end of DisplayBox.Canvas
