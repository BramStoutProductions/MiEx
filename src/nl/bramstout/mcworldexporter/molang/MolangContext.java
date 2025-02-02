package nl.bramstout.mcworldexporter.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import nl.bramstout.mcworldexporter.Random;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangDictionary;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangNull;

public class MolangContext {
	
	private static class Scope{
		
		public MolangValue prevTempValues;
		
		public Scope(MolangValue prevTempValues) {
			this.prevTempValues = prevTempValues;
		}
		
	}
	
	private Map<String, MolangValue> globals;
	private MolangValue tempValues;
	private Map<String, String> aliases;
	private boolean breakFlag;
	private boolean continueFlag;
	private boolean returnFlag;
	private MolangValue returnValue;
	private Stack<Scope> scopes;
	private Stack<AnimationInfo> animationInfos;
	
	public MolangContext(MolangQuery query, Random random) {
		this.globals = new HashMap<String, MolangValue>();
		this.globals.put("math", new MolangValue(new MolangMath(random)));
		this.globals.put("query", new MolangValue(query));
		this.globals.put("variable", new MolangValue(new MolangDictionary()));
		this.tempValues = new MolangValue(new MolangDictionary());
		this.aliases = new HashMap<String, String>();
		this.aliases.put("c", "context");
		this.aliases.put("q", "query");
		this.aliases.put("t", "temp");
		this.aliases.put("v", "variable");
		this.scopes = new Stack<Scope>();
		this.animationInfos = new Stack<AnimationInfo>();
		
		this.breakFlag = false;
		this.continueFlag = false;
		this.returnFlag = false;
		this.returnValue = new MolangValue(new MolangNull());
	}
	
	public MolangContext(MolangContext other) {
		this.globals = other.globals;
		this.tempValues = new MolangValue(new MolangDictionary());
		this.aliases = other.aliases;
		this.scopes = new Stack<Scope>();
		this.animationInfos = new Stack<AnimationInfo>();
		this.animationInfos.addAll(animationInfos);
		
		this.breakFlag = false;
		this.continueFlag = false;
		this.returnFlag = false;
		this.returnValue = new MolangValue(new MolangNull());
	}
	
	public MolangContext copy() {
		return new MolangContext(this);
	}
	
	public void startScope() {
		scopes.add(new Scope(tempValues));
		tempValues = tempValues.copy();
	}
	
	public void endScope() {
		if(scopes.empty())
			return;
		Scope scope = scopes.pop();
		tempValues = scope.prevTempValues;
	}
	
	public AnimationInfo getAnimationInfo() {
		if(animationInfos.isEmpty())
			return null;
		return animationInfos.peek();
	}
	
	public AnimationInfo pushAnimationInfo() {
		AnimationInfo info = null;
		if(animationInfos.isEmpty())
			info = new AnimationInfo();
		else
			info = new AnimationInfo(animationInfos.peek());
		animationInfos.push(info);
		return info;
	}
	
	public void popAnimationInfo() {
		animationInfos.pop();
	}
	
	public MolangValue getGlobal(String name) {
		if(name.equals("temp") || name.equals("t"))
			return tempValues;
		return globals.getOrDefault(name, globals.getOrDefault(aliases.getOrDefault(name, ""), null));
	}
	
	public void setBreakFlag(boolean val) {
		this.breakFlag = val;
	}
	
	public void setContinueFlag(boolean val) {
		this.continueFlag = val;
	}
	
	public boolean getBreakFlag() {
		return breakFlag;
	}
	
	public boolean getContinueFlag() {
		return continueFlag;
	}
	
	public boolean getReturnFlag() {
		return returnFlag;
	}
	
	public void setReturnValue(MolangValue value) {
		this.returnValue = value;
		this.returnFlag = true;
	}
	
	public void clearReturnValue() {
		this.returnFlag = false;
		this.returnValue = new MolangValue(new MolangNull());
	}
	
	public MolangValue getReturnValue() {
		return this.returnValue;
	}
	
	public MolangValue getTempDict() {
		return tempValues;
	}
	
	public void setVariableDict(MolangValue variableDict) {
		globals.put("variable", variableDict);
	}
	
	public MolangValue getVariableDict() {
		return globals.getOrDefault("variable", null);
	}
	
	public void setContextDict(MolangValue contextDict) {
		globals.put("context", contextDict);
	}
	
	public void setGlobal(String name, MolangValue value) {
		globals.put(name, value);
	}
	
}
