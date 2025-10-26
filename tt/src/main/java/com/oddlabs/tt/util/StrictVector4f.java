/* 
 * Copyright (c) 2002-2004 LWJGL Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are 
 * met:
 * 
 * * Redistributions of source code must retain the above copyright 
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of 
 *   its contributors may be used to endorse or promote products derived 
 *   from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.FloatBuffer;

/**
 * $Id: StrictVector4f.java,v 1.3 2004/06/12 20:28:29 matzon Exp $
 * <p>
 * Holds a 4-tuple vector.
 * 
 * @version $Revision: 1.3 $
 */

public class StrictVector4f {

	public float x, y, z, w;
	
	/**
	 * Constructor
	 */
	public StrictVector4f() {
	}

	/**
	 * Constructor
	 */
	public StrictVector4f(float x, float y, float z, float w) {
		set(x, y, z, w);
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector2f#set(float, float)
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableVector3f#set(float, float, float)
	 */
	public void set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.util.vector.WritableStrictVector4f#set(float, float, float, float)
	 */
	public void set(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	/**
	 * @return the length squared of the vector
	 */
	public float lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}

	/**
	 * Translate a vector
	 * @param x The translation in x
	 * @param y the translation in y
	 * @return this
	 */
	public @NonNull StrictVector4f translate(float x, float y, float z, float w) {
		this.x += x;
		this.y += y;
		this.z += z;
		this.w += w;
		return this;
	}

	/**
	 * Add a vector to another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the sum of left and right in dest
	 */
	public static @NonNull StrictVector4f add(@NonNull StrictVector4f left, @NonNull StrictVector4f right, @Nullable StrictVector4f dest) {
		if (dest == null)
			return new StrictVector4f(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z, left.w + right.w);
			return dest;
		}
	}

	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return left minus right in dest
	 */
	public static @NonNull StrictVector4f sub(@NonNull StrictVector4f left, @NonNull StrictVector4f right, @Nullable StrictVector4f dest) {
		if (dest == null)
			return new StrictVector4f(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z, left.w - right.w);
			return dest;
		}
	}


	/**
	 * Negate a vector
	 * @return this
	 */
	public @NonNull StrictVector4f negate() {
		x = -x;
		y = -y;
		z = -z;
		w = -w;
		return this;
	}

	/**
	 * Negate a vector and place the result in a destination vector.
	 * @param dest The destination vector or null if a new vector is to be created
	 * @return the negated vector
	 */
	public @NonNull StrictVector4f negate(@Nullable StrictVector4f dest) {
		if (dest == null)
			dest = new StrictVector4f();
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		dest.w = -w;
		return dest;
	}

	/**
	 * @return the length of the vector
	 */
	public final float length() {
		return (float) Math.sqrt(lengthSquared());
	}


	/**
	 * Normalise this vector
	 * @return this
	 */
	public final StrictVector4f normalise() {
		float len = length();
		if (len != 0.0f) {
			float l = 1.0f / len;
			return scale(l);
		} else
			throw new IllegalStateException("Zero length vector");
	}

	/**
	 * Normalise this vector and place the result in another vector.
	 * @param dest The destination vector, or null if a new vector is to be created
	 * @return the normalised vector
	 */
	public @NonNull StrictVector4f normalise(@Nullable StrictVector4f dest) {
		float l = length();

		if (dest == null)
			dest = new StrictVector4f(x / l, y / l, z / l, w / l);
		else
			dest.set(x / l, y / l, z / l, w / l);

		return dest;
	}

	/**
	 * The dot product of two vectors is calculated as
	 * v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1.w * v2.w
	 * @param left The LHS vector
	 * @param right The RHS vector
	 * @return left dot right
	 */
	public static float dot(@NonNull StrictVector4f left, @NonNull StrictVector4f right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}

	/**
	 * Calculate the angle between two vectors, in degrees
	 * @param a A vector
	 * @param b The other vector
	 * @return the angle between the two vectors, in degrees
	 */
	public static float angle(@NonNull StrictVector4f a, @NonNull StrictVector4f b) {
		float dls = dot(a, b) / (a.length() * b.length());
		if (dls < -1f)
			dls = -1f;
		else if (dls > 1.0f)
			dls = 1.0f;
		return (float) Math.toDegrees(Math.acos(dls));
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.StrictVector4f#load(FloatBuffer)
	 */
	public @NonNull StrictVector4f load(@NonNull FloatBuffer buf) {
		x = buf.get();
		y = buf.get();
		z = buf.get();
		w = buf.get();
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.StrictVector4f#scale(float)
	 */
	public @NonNull StrictVector4f scale(float scale) {
		x *= scale;
		y *= scale;
		z *= scale;
		w *= scale;
		return this;
	}

	/* (non-Javadoc)
	 * @see org.lwjgl.vector.StrictVector4f#store(FloatBuffer)
	 */
	public @NonNull StrictVector4f store(@NonNull FloatBuffer buf) {

		buf.put(x);
		buf.put(y);
		buf.put(z);
		buf.put(w);

		return this;
	}

        @Override
	public @NonNull String toString() {
		return "StrictVector4f: " + x + " " + y + " " + z + " " + w;
	}

	/**
	 * @return x
	 */
	public final float getX() {
		return x;
	}

	/**
	 * @return y
	 */
	public final float getY() {
		return y;
	}

	/**
	 * Set X
     */
	public final void setX(float x) {
		this.x = x;
	}

	/**
	 * Set Y
     */
	public final void setY(float y) {
		this.y = y;
	}

	/**
	 * Set Z
     */
	public void setZ(float z) {
		this.z = z;
	}


	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Set W
     */
	public void setW(float w) {
		this.w = w;
	}

	/* (Overrides)
	 * @see org.lwjgl.vector.ReadableVector3f#getZ()
	 */
	public float getW() {
		return w;
	}


}
