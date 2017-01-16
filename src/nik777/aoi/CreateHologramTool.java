/*  CreateHologramTool.java  */

package nik777.aoi.hologram;

/*
 * CreateHologramTool: EditingTool for creating new Hologram objects
 *
 * Copyright (C) 2008 Nik Trevallyn-Jones, Sydney, Australia
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See version 2 of the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, version 2 of the license is available
 * from the GNU project, at http://www.gnu.org.
 */

/*
 * This is a derivative work, based on the code in CreatePolymeshTool, which
 * is copyright (C) Francois Guillet.
 */

import java.awt.Point;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.animation.PositionTrack;
import artofillusion.animation.RotationTrack;
import artofillusion.math.*;
import artofillusion.texture.Texture;
import artofillusion.ui.*;
import buoy.event.WidgetMouseEvent;

/**
 * The editing tool which creates a Hologram
 * @author Nik Trevallyn-Jones
 */
public class CreateHologramTool extends EditingTool
{
    /** track the number - for naming */
    public static int hologramCounter = 1;
    
    /** start of drag region  */
    protected Point startDrag;

    /** Create a new CreateHologramTool */
    public CreateHologramTool(EditingWindow parent)
    {
	super(parent);
	initButton("Hologram:createHologram");
    }
	
    /** Return tool tip */
    public String getToolTipText()
    {
	return Translate.text("Hologram:createHologram.toolTip");
    }
	
    /**
     * respond to tool activation
     */
    public void activate()
    {
	super.activate();
	theWindow.setHelpText(Translate.text("Hologram:createHologram.toolHelp"));
    }
	
    /**
     * Tell AoI which clicks we want to catch
     */
    public int whichClicks()
    { return ALL_CLICKS; }
	
    /**
     * respond to mouse clicks
     */
    public void mousePressed(WidgetMouseEvent ev, ViewerCanvas view)
    {
        startDrag = ev.getPoint();
        ((SceneViewer) view).beginDraggingBox(startDrag, ev.isShiftDown());
    }

    /**
     */
    public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
    {
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        Camera cam = view.getCamera();
        Point endDrag = e.getPoint();
        Vec3 v1, v2, v3, orig, xdir, ydir, zdir;
        double xsize, ysize, zsize;
        int i;
        
        if (endDrag.x == startDrag.x || endDrag.y == startDrag.y)
        {
            ((SceneViewer) view).repaint();
            return;
        }

        v1 = cam.convertScreenToWorld(startDrag, Camera.DEFAULT_DISTANCE_TO_SCREEN);
        v2 = cam.convertScreenToWorld(new Point(endDrag.x, startDrag.y),
				      Camera.DEFAULT_DISTANCE_TO_SCREEN);
        v3 = cam.convertScreenToWorld(endDrag, Camera.DEFAULT_DISTANCE_TO_SCREEN);
        orig = v1.plus(v3).times(0.5);
        if (endDrag.x < startDrag.x)
            xdir = v1.minus(v2);
        else
            xdir = v2.minus(v1);
        if (endDrag.y < startDrag.y)
            ydir = v3.minus(v2);
        else
            ydir = v2.minus(v3);
        xsize = xdir.length();
        ysize = ydir.length();
        xdir = xdir.times(1.0/xsize);
        ydir = ydir.times(1.0/ysize);

        zdir = xdir.cross(ydir);
        zsize = Math.min(xsize, ysize);
        
	HologramObject holo = new HologramObject();
	Texture tex = view.getScene().getDefaultTexture();
	holo.setTexture(tex, tex.getDefaultMapping(holo));
	holo.setSize(xsize, ysize, 0);

	ObjectInfo info = new ObjectInfo(holo, new CoordinateSystem(orig, Vec3.vz(), Vec3.vy()), "Hologram " + (hologramCounter++));

	LayoutWindow window = (LayoutWindow) theWindow;
	//window.addObject(info, null);
	//window.updateImage();

        //ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(orig, zdir, ydir), "Hologram "+(hologramCounter++));

        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);

        UndoRecord undo = new UndoRecord(theWindow, false);
        undo.addCommandAtBeginning(UndoRecord.SET_SCENE_SELECTION, new Object [] {theScene.getSelection()});
        window.addObject(info, undo);
        window.setUndoRecord(undo);
        window.setSelection(theScene.getNumObjects()-1);
        window.updateImage();
    }
}
