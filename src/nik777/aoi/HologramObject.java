/*  HologramObject.java  */

package nik777.aoi.hologram;

/*
 * HologramObject: Object which fakes a 3D projection from 2D info
 *
 * Copyright (C) 2005, Nik Trevallyn-Jones, Sydney, Australia
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
 * with this program. If not, the license, including version 2, is available
 * from the GNU project, at http://www.gnu.org.
 */

import artofillusion.*;
import artofillusion.texture.*;
import artofillusion.animation.*;
import artofillusion.math.BoundingBox;
import artofillusion.math.RGBColor;
import artofillusion.math.Vec2;
import artofillusion.object.Object3D;
import artofillusion.object.ObjectWrapper;
import artofillusion.object.Cylinder;
import artofillusion.object.Cube;
import artofillusion.object.ObjectInfo;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 */

public class HologramObject extends ObjectWrapper
{
    //protected Object3D theObject;
    protected Cylinder projection;

    protected static UniformTexture projtex = null;
    private static float VERSION_NUMBER = 1.0f;

    public HologramObject()
    {
	theObject = new Cube(1.0, 1.0, 0.0);
	texParam = new TextureParameter[0];
	paramValue = new ParameterValue[0];

	/*
	Texture tex = new ProceduralTexture2D();

	setTexture(tex, new ProjectionMapping(theObject, tex));
	setParameters(texParam);
	setParameterValues(paramValue);
	*/
    }

    public Object3D duplicate()
    {
	HologramObject result = new HologramObject();
	result.copyObject(this);

	return result;
    }
  
    public void copyObject(Object3D obj)
    {
	if (obj instanceof ObjectWrapper)
	    obj = ((ObjectWrapper) obj).getWrappedObject();

	theObject = obj.duplicate();

	Texture tex = obj.getTexture();
	setTexture(tex, obj.getTextureMapping().duplicate(theObject, tex));

	double width = ((Double) theObject.getPropertyValue(0)).doubleValue();
	double height = ((Double) theObject.getPropertyValue(1)).doubleValue();

	projection.setSize(width, height, width);
    }

    public boolean canSetTexture()
    { return true; }

    public void setTexture(Texture tex, TextureMapping map)
    {
	/*
	if (map == null) map = new HologramMapping(tex);
	else if (!(map instanceof HologramMapping)) {
	    map = new HologramMapping(map);
	}
	*/
	if (map == null) map = new ProjectionMapping(theObject, tex);

	theObject.setTexture(tex, map);

	double width = ((Double) theObject.getPropertyValue(0)).doubleValue();
	double height = ((Double) theObject.getPropertyValue(1)).doubleValue();

	if (map instanceof ProjectionMapping) {
	    ((ProjectionMapping) map).setScale(new Vec2(width, height));
	    ((ProjectionMapping) map).setScaledToObject(false);
	}

	// make sure projection is initialised...
	if (projection == null) {
	    // how non-rendering views see this object
	    /*
	    double width = ((Double)
			    theObject.getPropertyValue(0)).doubleValue();

	    double height = ((Double)
			     theObject.getPropertyValue(1)).doubleValue();
	    */

	    projection = new Cylinder(height, width/2, width/2, 1.0);

	    if (projtex == null) {
		projtex = new UniformTexture();
		projtex.diffuseColor = new RGBColor(0.5, 0.5, 0.75);
	    }

	    projection.setTexture(projtex,
				  new UniformMapping(theObject, projtex));

	    projection.setParameters(new TextureParameter[0]);
	    projection.setParameterValues(new ParameterValue[0]);
	}
    }

    public int canConvertToTriangleMesh()
    { return Object3D.CANT_CONVERT; }

    public boolean canSetMaterial()
    { return false; }

    public BoundingBox getBounds()
    { return projection.getBounds(); }

    public Property[] getProperties()
    { return theObject.getProperties(); }

    public Object getPropertyValue(int index)
    { return theObject.getPropertyValue(index); }

    public void setPropertyValue(int index, Object value)
    {
	theObject.setPropertyValue(index, value);

	double width = ((Double) theObject.getPropertyValue(0)).doubleValue();
	double height = ((Double) theObject.getPropertyValue(1)).doubleValue();

	projection.setSize(width, height, width);
    }
  
    public void setSize(double xsize, double ysize, double zsize)
    {
	theObject.setSize(xsize, ysize, zsize);
	projection.setSize(xsize, ysize, zsize);

	TextureMapping map = theObject.getTextureMapping();
	if (map instanceof ProjectionMapping)
	    ((ProjectionMapping) map).setScale(new Vec2(xsize, ysize));
    }

    public RenderingMesh getRenderingMesh(double tol, boolean interactive,
					  ObjectInfo info)
    { return projection.getRenderingMesh(tol, interactive, info); }

    public WireframeMesh getWireframeMesh()
    { return projection.getWireframeMesh(); }

    public Keyframe getPoseKeyframe()
    { return theObject.getPoseKeyframe(); }
  
    public void applyPoseKeyframe(Keyframe k)
    { theObject.applyPoseKeyframe(k); }

    public void writeToFile(DataOutputStream out, Scene theScene)
	throws IOException
    {
	// write the object format version
	out.writeUTF(String.valueOf(VERSION_NUMBER));

	if (theObject != null) {
	    out.writeUTF(theObject.getClass().getName());
	    theObject.writeToFile(out, theScene);
	}
	else out.writeUTF("*EMPTY*");
    }
  
    public HologramObject(DataInputStream in, Scene theScene)
	throws IOException, InvalidObjectException
    {
	float vers = 0.0f;
	
	String typename = null;

	String versno = in.readUTF();
	try {
	    vers = Float.parseFloat(versno);
	} catch (Exception e) {
	    // not a version number
	    // so it's the typename in the original format
	    typename = versno;
	}

	// check we can read this version
	if (vers > VERSION_NUMBER) {
	    System.out.println("HologramObject: unrecognised version: "
			       + vers);
	    throw new
		InvalidObjectException("HologramObject: invalid version: "
				       + vers);
	}

	if (typename == null) typename = in.readUTF();
	if (typename.indexOf('.') > 0) {

	    try {
		Class type = ModellingApp.getClass(typename);
		Constructor con = type.getConstructor(new Class []
		    { DataInputStream.class, Scene.class });

		theObject = (Object3D) con.newInstance(new Object []
		    {in, theScene});

		setTexture(theObject.getTexture(),
			   theObject.getTextureMapping());

		/*
		double width = ((Double) theObject.getPropertyValue(0))
		    .doubleValue();
		double height = ((Double) theObject.getPropertyValue(1))
		    .doubleValue();

		projection = new Cylinder(height, width/2, width/2, 1.0);

		UniformTexture projtex = new UniformTexture();
		projtex.diffuseColor = new RGBColor(0.5, 0.5, 0.75);
		
		projection.setTexture(projtex, 
				      new UniformMapping(theObject, projtex));

		projection.setParameters(new TextureParameter[0]);
		projection.setParameterValues(new ParameterValue[0]);
		*/

	    } catch (Exception e) {
		System.out.println("HologramObject.ctor: " + e);
		throw new InvalidObjectException(e.toString());
	    }
	}
    }
}