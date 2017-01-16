/*  HologramPlugin  */

package nik777.aoi.hologram;

/*
 * HologramPlugin: Plugin for Hologram objects (3d projected from 2D data)
 *
 * Copyright (C) 2007 Nik Trevallyn-Jones, Sydney, Australia
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

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.Texture;
import artofillusion.ui.ToolPalette;
import artofillusion.ui.Translate;

import artofillusion.raytracer.RTObjectFactory;

import buoy.widget.*;
import buoy.event.*;

import java.util.Collection;

/**
 */

public class HologramPlugin implements Plugin, ModellingTool, RTObjectFactory
{
    private static String name = null;

    /**
     *  return tool name for ModellingTool interface
     */
    public String getName()
    {
	if (name == null) name = Translate.text("Hologram:name");
	return name;
    }

    /**
     * respond to plugin events
     */
    public void processMessage(int msg, Object[] args)
    {
	switch (msg) {
	case Plugin.SCENE_WINDOW_CREATED:
            LayoutWindow layout = (LayoutWindow) args[0];
            ToolPalette palette = layout.getToolPalette();
            palette.addTool(new CreateHologramTool(layout));
            break;
	}
    }
    
    /**
     * show dialog when selected from the menu 
     */
    public void commandSelected(LayoutWindow window)
    {
	Scene scene = window.getScene();

	HologramObject holo = new HologramObject();
	Texture tex = scene.getDefaultTexture();
	holo.setTexture(tex, tex.getDefaultMapping(holo));

	ObjectInfo info = new ObjectInfo(holo, new CoordinateSystem(new Vec3(0.0f, 0.0f, 0.0f), Vec3.vz(), Vec3.vy()), "Hologram");

	window.addObject(info, null);
	window.updateImage();
    }

    public boolean processObject(ObjectInfo info, Scene scene, Camera camera,
				 Collection rtobjects, Collection lights)
    {
	// we only process HologramObjects
	if (info.object.getClass() != HologramObject.class) return false;

	Object3D obj = info.object;
	CoordinateSystem coords = info.coords;
	rtobjects.add(new RTHologram(obj, coords.fromLocal(), coords.toLocal(),
				     obj.getAverageParameterValues()));

	return true;
    }
}