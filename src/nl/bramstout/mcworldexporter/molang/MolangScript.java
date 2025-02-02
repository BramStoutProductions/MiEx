package nl.bramstout.mcworldexporter.molang;

import java.util.List;

import nl.bramstout.mcworldexporter.molang.MolangValue.MolangNull;

public class MolangScript extends MolangExpression{

	private String originalCode;
	private List<MolangExpression> expressions;
	
	public MolangScript(List<MolangExpression> expressions) {
		originalCode = "";
		this.expressions = expressions;
	}
	
	public void setOriginalCode(String code) {
		this.originalCode = code;
	}
	
	@Override
	public MolangValue eval(MolangContext context) {
		MolangValue returnValue = new MolangValue(new MolangNull());
		try {
			if(context.getReturnFlag())
				return context.getReturnValue();
			context.startScope();
			for(MolangExpression expr : expressions) {
				returnValue = expr.eval(context);
				if(context.getReturnFlag()) {
					context.endScope();
					return context.getReturnValue();
				}
			}
			context.endScope();
		}catch(Exception ex) {
			System.err.println(originalCode);
			ex.printStackTrace();
		}
		return returnValue;
	}
	
}
