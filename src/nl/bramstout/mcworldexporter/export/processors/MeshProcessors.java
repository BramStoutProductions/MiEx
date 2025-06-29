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

package nl.bramstout.mcworldexporter.export.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.bramstout.mcworldexporter.export.Mesh;
import nl.bramstout.mcworldexporter.export.MeshPurpose;

public class MeshProcessors {
	
	public static interface IMeshProcessor{
		
		public void process(Mesh mesh, MeshProcessors manager) throws Exception;
		
	}
	
	public static class WriteCapturer{
		
		public List<Mesh> meshes = new ArrayList<Mesh>();
		
	}
	
	public static enum MeshMergerMode{
		DISABLED, MERGE
	}
	
	public static class MeshMerger{
		
		private static class MeshKind{
			public boolean doubleSided;
			public MeshPurpose purpose;
			
			public MeshKind(Mesh mesh) {
				doubleSided = mesh.isDoubleSided();
				purpose = mesh.getPurpose();
			}
			
			@Override
			public boolean equals(Object obj) {
				if(!(obj instanceof MeshKind))
					return false;
				return ((MeshKind) obj).doubleSided == doubleSided && ((MeshKind) obj).purpose == purpose;
			}
			
			@Override
			public int hashCode() {
				return purpose.id * 2 + (doubleSided ? 1 : 0);
			}
		}
		
		public List<Mesh> meshes = new ArrayList<Mesh>();
		public MeshMergerMode mode = MeshMergerMode.DISABLED;
		public IMeshProcessor nextProcessor = null;
		
		public void handle(MeshProcessors manager) throws Exception {
			IMeshProcessor nextProcessor2 = nextProcessor;
			if(nextProcessor2 == null)
				nextProcessor2 = manager.getDefaultWriteProcessor();
			if(mode == MeshMergerMode.DISABLED)
				return;
			else if(mode == MeshMergerMode.MERGE) {
				Map<MeshKind, Mesh> combinedMeshes = new HashMap<MeshKind, Mesh>();
				for(Mesh mesh : meshes) {
					MeshKind kind = new MeshKind(mesh);
					Mesh combinedMesh = combinedMeshes.getOrDefault(kind, null);
					if(combinedMesh == null) {
						combinedMesh = new Mesh(manager.meshNamePrefix + "_" + kind.hashCode(), kind.purpose, 
												mesh.getTexture(), mesh.getMatTexture(), mesh.hasAnimatedTexture(), kind.doubleSided, 64, 16);
						combinedMeshes.put(kind, combinedMesh);
					}
					combinedMesh.appendMesh(mesh, true);
				}
				for(Mesh combinedMesh : combinedMeshes.values()) {
					if(nextProcessor2 != null)
						nextProcessor2.process(combinedMesh, manager);
				}
			}
		}
		
	}
	
	private List<IMeshProcessor> processorChain;
	private List<WriteCapturer> writeCapturers;
	private List<MeshMerger> meshMergers;
	private WriteProcessor defaultWriteProcessor;
	private String meshNamePrefix;
	
	public MeshProcessors(String meshNamePrefix) {
		this.processorChain = new ArrayList<IMeshProcessor>();
		this.writeCapturers = new ArrayList<WriteCapturer>();
		this.meshMergers = new ArrayList<MeshMerger>();
		this.defaultWriteProcessor = null;
		this.meshNamePrefix = meshNamePrefix;
	}
	
	public int registerWriteCapturer() {
		for(int i = 0; i < writeCapturers.size(); ++i) {
			if(writeCapturers.get(i) == null) {
				writeCapturers.set(i, new WriteCapturer());
				return i;
			}
		}
		writeCapturers.add(new WriteCapturer());
		return writeCapturers.size() - 1;
	}
	
	public void unregisterWriteCapturer(int id) {
		if(id < 0 || id >= writeCapturers.size())
			return;
		writeCapturers.set(id, null);
	}
	
	public WriteCapturer getWriteCapturer(int id) {
		if(id < 0 || id >= writeCapturers.size())
			return null;
		return writeCapturers.get(id);
	}
	
	public int beginMeshMerger(MeshMergerMode mode) {
		return beginMeshMerger(mode, null);
	}
	
	public int beginMeshMerger(MeshMergerMode mode, IMeshProcessor nextProcessor) {
		MeshMerger merger = new MeshMerger();
		merger.mode = mode;
		merger.nextProcessor = nextProcessor;
		meshMergers.add(merger);
		return meshMergers.size() - 1;
	}
	
	public void endMeshMerger(int id) throws Exception {
		if(id < 0 || id >= meshMergers.size())
			return;
		for(int i = meshMergers.size()-1; i >= id; i--) {
			MeshMerger merger = meshMergers.remove(i);
			merger.handle(this);
		}
	}
	
	public MeshMerger getCurrentMeshMerger() {
		if(meshMergers.size() == 0)
			return null;
		return meshMergers.get(meshMergers.size() - 1);
	}
	
	public void meshWritten(Mesh mesh) {
		for(WriteCapturer capturer : writeCapturers)
			if(capturer != null)
				capturer.meshes.add(mesh);
	}
	
	public MeshProcessors addProcessor(IMeshProcessor processor) {
		processorChain.add(processor);
		if(defaultWriteProcessor == null && processor instanceof WriteProcessor)
			defaultWriteProcessor = (WriteProcessor) processor;
		return this;
	}
	
	public WriteProcessor getDefaultWriteProcessor() {
		return defaultWriteProcessor;
	}
	
	public void setDefaultWriteProcessor(WriteProcessor processor) {
		this.defaultWriteProcessor = processor;
	}
	
	public void process(Mesh mesh) throws Exception{
		if(processorChain.size() == 0)
			return;
		processorChain.get(0).process(mesh, this);
	}
	
	protected void processNext(Mesh mesh, IMeshProcessor currentProcessor) throws Exception {
		boolean found = false;
		for(int i = 0; i < processorChain.size(); ++i) {
			if(processorChain.get(i) == currentProcessor) {
				found = true;
				continue;
			}
			if(found) {
				processorChain.get(i).process(mesh, this);
				return;
			}
		}
	}
	
	public IMeshProcessor getNextProcessor(IMeshProcessor currentProcessor) {
		IMeshProcessor nextProcessor = null;
		boolean found = false;
		for(int i = 0; i < processorChain.size(); ++i) {
			if(processorChain.get(i) == currentProcessor) {
				found = true;
				continue;
			}
			if(found) {
				nextProcessor = processorChain.get(i);
				break;
			}
		}
		return nextProcessor;
	}
	
}
