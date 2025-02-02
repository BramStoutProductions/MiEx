/*
 * BSD 3-Clause License
 * 
 * Copyright (c) 2024, Bram Stout Productions
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nl.bramstout.mcworldexporter.pbr.nodes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public abstract class PbrNode {
	
	private String name;
	private PbrNodeGraph graph;
	
	protected PbrNode(String name, PbrNodeGraph graph) {
		this.name = name;
		this.graph = graph;
	}
	
	public String getName() {
		return name;
	}
	
	public PbrNodeGraph getGraph() {
		return graph;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<PbrAttribute> getAttributes() {
		List<PbrAttribute> res = new ArrayList<PbrAttribute>();
		getAttributes(res);
		return res;
	}
	
	public void getAttributes(List<PbrAttribute> res) {
		for(Field field : this.getClass().getFields()) {
			if(PbrAttribute.class.isAssignableFrom(field.getType())) {
				try {
					res.add((PbrAttribute) field.get(this));
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public PbrAttribute getAttribute(String name) {
		for(Field field : this.getClass().getFields()) {
			if(PbrAttribute.class.isAssignableFrom(field.getType())) {
				if(field.getName().equals(name)) {
					try {
						return (PbrAttribute) field.get(this);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}
		return null;
	}
	
	public String getAttributeName(PbrAttribute attr) {
		for(Field field : this.getClass().getFields()) {
			if(PbrAttribute.class.isAssignableFrom(field.getType())) {
				try {
					PbrAttribute attr2 = (PbrAttribute) field.get(this);
					if(attr == attr2)
						return field.getName();
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return "";
	}

	/**
	 * Called when the given attribute is flagged as dirty.
	 * @param attr
	 */
	public abstract void attributeDirty(PbrAttribute attr, PbrContext context);
	
	/**
	 * Called when the given output attribute needs to be evaluated.
	 * @param attr
	 */
	public abstract void evaluate(PbrAttribute attr, PbrContext context);
	
	public abstract PbrNode newInstanceOfSameType(PbrNodeGraph graph);
	
	public void copyFrom(PbrNode other, PbrNodeGraph currentNodeGraph) {
		List<PbrAttribute> attrs = getAttributes();
		PbrAttribute attr = null;
		PbrAttribute otherAttr = null;
		for(int i = 0; i < attrs.size(); ++i) {
			attr = attrs.get(i);
			otherAttr = other.getAttribute(attr.getName());
			if(otherAttr == null)
				throw new RuntimeException("Other attribute does not have the same attribute with name: " + attr.getName());
			attr.copyFrom(otherAttr, currentNodeGraph);
		}
	}
	
}
