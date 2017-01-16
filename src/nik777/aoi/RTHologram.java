/*  RTHologram.java  */

//package artofillusion.raytracer;
package nik777.aoi.hologram;

/*
 * RTHologram: RayTracer support for HologramObjects
 *
 * Copyright (C) 2007 Nik Trevallyn-Jones, Sydney Australia.
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

import artofillusion.raytracer.*;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.material.*;

import buoy.widget.*;

import java.io.*;

/**
 */

public class RTHologram extends RTObject
{
    protected ObjectWrapper theObject;
    protected CoordinateSystem coords;
    protected Cube cube;
    protected Mat4 fromLocal, toLocal;
    protected double param[];
    protected BoundingBox bounds;
    protected ThreadLocal context;

    protected static final Vec3 ZDIR = Vec3.vz();
    protected static final Vec3 YDIR = Vec3.vy();

    // NTJ: debug
    protected boolean debugged = false;

    public RTHologram(Object3D obj, Mat4 fromLocal, Mat4 toLocal,
			double param[])
    {
	theObject = (ObjectWrapper) obj;
	this.fromLocal = fromLocal;
	this.toLocal = toLocal;
	this.param = param;

	context = new ThreadLocal() {
		protected synchronized Object initialValue()
		{
		    Object cx[] = new Object[3];
		    cx[0] = coords.duplicate();
		    cx[1] = coords.duplicate();
		    cx[2] = new Vec3();

		    return cx;
		}
	    };

	cube = (Cube) theObject.getWrappedObject();

	coords = new CoordinateSystem();
	coords.transformCoordinates(fromLocal);

	BoundingBox bb = cube.getBounds();

	bounds = new BoundingBox(bb.minx, bb.maxx, bb.miny, bb.maxy,
				 bb.minx, bb.maxx);
    }

    /**
     * Get the TextureMapping for this object.
     */
    public TextureMapping getTextureMapping()
    { return theObject.getTextureMapping(); }

    /**
     * Get the MaterialMapping for this object.
     */
    public MaterialMapping getMaterialMapping()
    { return theObject.getMaterialMapping(); }
  
    /** 
     *  Determine whether a ray intersects this object.
     *
     *  This defers this to the nested Cube object, after first rotating
     *  the cube to face the origin of the ray.
     */
    public SurfaceIntersection checkIntersection(Ray r)
    {
	Vec3 dir = r.direction;

	// for local copies of various Vec3 values.
	Vec3 orig = r.tempVec1;
	Vec3 rorig = r.tempVec2;
	Vec3 rdir = r.tempVec3;
	Vec3 point = r.tempVec4;

	// get the threadlocal context
	Object[] cx = (Object[]) context.get();

	CoordinateSystem scc = (CoordinateSystem) cx[0];
	CoordinateSystem lcc = (CoordinateSystem) cx[1];
	Vec3 intersect = (Vec3) cx[2];

	// avoid casting shadows on ourselves
	if (r.origin.equals(intersect))
	    return SurfaceIntersection.NO_INTERSECTION;

	scc.copyCoords(coords);
	lcc.copyCoords(coords);

	// copy the two origins
	orig.set(coords.getOrigin());		// coord (cube) origin
	toLocal.transform(orig);

	rorig.set(r.origin);			// ray origin
	toLocal.transform(rorig);

	// project them both onto the XZ plane
	orig.y = 0.0;
	rorig.y = 0.0;

	// calculate the vector *from* cube *to* ray origin
	rdir.set(rorig.minus(orig));
	rdir.normalize();

	// get the angle between the projected vector and the cube's Z axis
	double angle = Math.acos(rdir.dot(ZDIR));
	//double angle = Math.acos(rdir.dot(coords.getZDirection()));
	
	// work out if the angle is negative
	if (rdir.x < 0) angle = -angle;

	// calculate the rotation of the coords
	//Mat4 rot = Mat4.axisRotation(YDIR, angle);

	// TESTING: try rotating about local Y axis
	Mat4 rot = Mat4.axisRotation(fromLocal.timesDirection(YDIR), angle);

	// rotate the coords
	scc.transformAxes(rot);

	// print debug once per frame...
	if (!debugged) {
	    System.out.println("\nRTHologram: dot=" + rdir.dot(Vec3.vz()) +
			       "; angle=" + angle +
			       "\norig=" + orig +
			       "\nrorig=" + rorig +
			       "\nrdir=" + rdir +
			       "\nviewdir=" + dir +
			       "\norig zdir=" +
			       coords.fromLocal().timesDirection(Vec3.vz()) +
			       "\nnew zdir=" +
			       scc.fromLocal().timesDirection(Vec3.vz()) +
			       "\nlocal zdir=" + scc.toLocal()
			       .timesDirection(scc.fromLocal()
					       .timesDirection(Vec3.vz()))
									      
			       );
	}

	// create a new RTCube which (should be) facing the ray origin
	RTCube surf = new RTCube(cube, scc.fromLocal(), scc.toLocal(), param);
	//RTCubeWrapper surf = new RTCubeWrapper(cube, scc.fromLocal(),
	//			       scc.toLocal(), param);

	// calculate the surface intersection on this cube	
	SurfaceIntersection si = surf.checkIntersection(r);
	//SurfaceIntersection si = surf.intersect(r);

	/*
	// NTJ: testing only intersect on front (textured) face
	if (si != SurfaceIntersection.NO_INTERSECTION) {

	    si.trueNormal(r.tempVec4);
	    
	    //System.out.println("trueNorm (g)= " + r.tempVec4);
	    scc.toLocal().transformDirection(r.tempVec4);
	    //System.out.println("trueNorm (l)= " + r.tempVec4);

	    if (r.tempVec4.x < 0) {
		//System.out.println("not fron face: " + r.tempVec4);
		si = SurfaceIntersection.NO_INTERSECTION;
	    }
	}
	*/

	// remember the intersection point
	if (si != SurfaceIntersection.NO_INTERSECTION)
	    si.intersectionPoint(0, intersect);


	/*	
	// avoid anomalies
	point.set(intersect);
	scc.toLocal().transform(point);

	if (point.z != 0 || point.y > bounds.maxy)
	  si = SurfaceIntersection.NO_INTERSECTION;
	*/

	/*
	 * We now repeat the process, but with the cube facing the ray,
	 * rather than the ray origin. We use this mapping for shadow
	 * rays.
	 * Currently disabled (code left in in case others wish to experiment)

	// transform ray direction to local coords and project onto XZ plane
	rdir.set(dir);
	toLocal.transformDirection(rdir);
	rdir.y = 0.0;
	rdir.normalize();

	double langle = Math.acos(rdir.dot(ZDIR));
	
	// work out if the angle is negative
	if (rdir.x < 0) langle = -langle;

	//	if (langle != angle)
	//  System.out.println("langle != angle. langle=" + langle +
	//	       "; angle=" + angle);

	// calculate the rotation of the coords
	rot = Mat4.axisRotation(YDIR, langle);

	// rotate the coords
	lcc.transformAxes(rot);

	RTCube light = new RTCube(cube, lcc.fromLocal(), lcc.toLocal(), param);
	SurfaceIntersection li = light.checkIntersection(r);

	*/

	// currently use the same mapping for both eye and shadow rays
	SurfaceIntersection li = si;

	// NTJ: debug	
	//si.intersectionPoint(0, pt);

	//System.out.println("RTHologram.checkIntersection: " + pt);

	// only debug once per render
	debugged = true;

	// return the surface intersection
	return (si == SurfaceIntersection.NO_INTERSECTION
		? si
		: new HologramIntersection(si, li));

	//return si;
    }

    /**
     *  Get a bounding box for this object.
     */
    public BoundingBox getBounds()
    { return bounds.transformAndOutset(fromLocal); }
    
    /**
     *  Determine whether any part of the object lies within a bounding box.
     */
    public boolean intersectsBox(BoundingBox bb)
    {
	if (!bb.intersects(getBounds()))
	    return false;
    
	// Check whether the box is entirely contained within this object.
    
	bb = bb.transformAndOutset(toLocal);
	if (bb.minx > bounds.minx && bb.maxx < bounds.maxx
	    && bb.miny > bounds.miny && bb.maxy < bounds.maxy
	    && bb.minz > bounds.minz && bb.maxz < bounds.maxz)
	    return false;

	return true;
    }
  
    /**
     *  Get the transformation from world coordinates to the object's 
     *  local coordinates.
     *
     *  Must dereference the ThreadLocal to ensure each thread sees the correct
     *  coords.
     */
    public Mat4 toLocal()
    { return ((CoordinateSystem[]) context.get())[0].toLocal(); }

    /**
     *  private intersection class
     *
     *  Based (slavishly) on RTCube.CubeIntersection.
     */  
    protected static class HologramIntersection implements SurfaceIntersection
    {
	private SurfaceIntersection si, li;

	public HologramIntersection(SurfaceIntersection si)
	{
	    this.si = si;
	    li = si;
	}

	public HologramIntersection(SurfaceIntersection si,
				    SurfaceIntersection li)
	{
	    this.si = si;
	    this.li = li;
	}

	public int numIntersections()
	{ return si.numIntersections(); }

	public void intersectionPoint(int n, Vec3 p)
	{ si.intersectionPoint(n, p); }

	public double intersectionDist(int n)
	{ return si.intersectionDist(n); }

	public void intersectionProperties(TextureSpec spec, Vec3 n,
					   Vec3 viewDir, double size,
					   double time)
	{
	    si.intersectionProperties(spec, n, viewDir, size, time);
	}

	public void intersectionTransparency(int n, RGBColor trans,
					     double angle, double size,
					     double time)
	{
	    li.intersectionTransparency(n, trans, angle, size, time);
	}

	public void trueNormal(Vec3 n)
	{
	    si.trueNormal(n);
	}
    }

    /**
     *  needed to access protected methods.
     */
    /*
    protected static class RTCubeWrapper extends RTCube
    {
	public RTCubeWrapper(Cube cube, Mat4 fromLocal, Mat4 toLocal,
			     double[] params)
	{ super(cube, fromLocal, toLocal, params); }

	public SurfaceIntersection intersect(Ray r)
	{ return super.checkIntersection(r); }
    }
    */
}
