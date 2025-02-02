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

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.pbr.PbrContext;
import nl.bramstout.mcworldexporter.pbr.PbrNodeGraph;

public abstract class PbrAttribute {

	private PbrNode node;
	private boolean dirtyFlag;
	private boolean isOutput;
	private boolean isContextSensitive;
	private PbrAttribute input;
	private List<PbrAttribute> outputs;
	
	protected PbrAttribute(PbrNode node, boolean isOutput, boolean isContextSensitive) {
		this.node = node;
		this.dirtyFlag = true;
		this.isOutput = isOutput;
		this.isContextSensitive = isContextSensitive;
		this.input = null;
		this.outputs = new ArrayList<PbrAttribute>();
	}
	
	protected void checkDirty(PbrContext context) {
		boolean isDirty = dirtyFlag;
		if(context != null && !isDirty)
			isDirty = context.dirtyAttributes.contains(this);
		if(isDirty) {
			if(isOutput) {
				node.evaluate(this, context);
			}
			else if(input != null) {
				setValue(input.getValue(context), context);
			}
			
			if(context == null)
				dirtyFlag = false;
			else
				context.dirtyAttributes.remove(this);
		}
	}
	
	public void notifyChange(PbrContext context) {
		if(context == null) {
			dirtyFlag = true;
		}else {
			context.dirtyAttributes.add(this);
		}
		node.attributeDirty(this, context);
		for(PbrAttribute output : outputs)
			output.notifyChange(context);
	}
	
	public PbrNode getNode() {
		return node;
	}
	
	public String getName() {
		return node.getAttributeName(this);
	}
	
	public boolean isOutput() {
		return isOutput;
	}
	
	public boolean isContextSensitive() {
		return isContextSensitive;
	}
	
	public boolean hasIncomingConnection() {
		return input != null;
	}
	
	public boolean hasOutgoingConnection() {
		return outputs.size() > 0;
	}
	
	public PbrAttribute getInput() {
		return input;
	}
	
	public List<PbrAttribute> getOutputs(){
		return outputs;
	}
	
	public void connect(PbrAttribute input) {
		if(isOutput)
			throw new RuntimeException("Output attributes cannot have incoming connections");
		if(input == null)
			disconnect();
		this.input = input;
		input.outputs.add(this);
		notifyChange(null);
	}
	
	public void disconnect() {
		if(this.input == null)
			return;
		this.input.outputs.remove(this);
		this.input = null;
		notifyChange(null);
	}
	
	public abstract Object getValue(PbrContext context);
	
	public abstract Object getDefaultValue();
	
	public abstract void setValue(Object value, PbrContext context);
	
	public void copyFrom(PbrAttribute other, PbrNodeGraph currentGraph) {
		dirtyFlag = other.dirtyFlag;
		if(other.input == null) {
			if(input != null) {
				disconnect();
			}
		}else {
			PbrNode node = currentGraph.getNode(other.input.getNode().getName());
			if(node == null)
				throw new RuntimeException("Could not find node with name: " + other.input.getNode().getName());
			PbrAttribute attr = node.getAttribute(other.input.getName());
			if(attr == null)
				throw new RuntimeException("Could not find attribute with name: " + other.input.getName() + 
						" in node: " + other.input.getNode().getName());
			connect(attr);
		}
	}
	
}
