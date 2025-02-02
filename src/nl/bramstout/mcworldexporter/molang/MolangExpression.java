package nl.bramstout.mcworldexporter.molang;

public abstract class MolangExpression {

	public static class MolangConstantExpression extends MolangExpression{
		
		private MolangValue value;
		
		public MolangConstantExpression(MolangValue value) {
			this.value = value;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return this.value;
		}
		
	}
	
	public static class MolangOperationExpression extends MolangExpression{
		
		private MolangOperation operation;
		
		public MolangOperationExpression(MolangOperation operation) {
			this.operation = operation;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(context.getReturnFlag())
				return context.getReturnValue();
			return operation.eval(context);
		}
		
		public MolangOperation getOperation() {
			return operation;
		}
		
	}
	
	public static class MolangParenthesesExpression extends MolangExpression{
		
		private MolangExpression subExpr;
		
		public MolangParenthesesExpression(MolangExpression subExpr) {
			this.subExpr = subExpr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(context.getReturnFlag())
				return context.getReturnValue();
			return subExpr.eval(context);
		}
		
	}
	
	public abstract MolangValue eval(MolangContext context);
	
}
