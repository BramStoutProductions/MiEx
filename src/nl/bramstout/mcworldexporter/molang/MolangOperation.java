package nl.bramstout.mcworldexporter.molang;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.molang.MolangValue.MolangArray;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangFunction;
import nl.bramstout.mcworldexporter.molang.MolangValue.MolangNull;

public abstract class MolangOperation {

	public static abstract class MolangOperationLeft extends MolangOperation{
		
		public MolangExpression leftExpr;
		
		public MolangOperationLeft(MolangExpression leftExpr) {
			this.leftExpr = leftExpr;
		}
		
	}
	
	public static abstract class MolangOperationRight extends MolangOperation{
		
		public MolangExpression rightExpr;
		
		public MolangOperationRight(MolangExpression rightExpr) {
			this.rightExpr = rightExpr;
		}
		
	}
	
	public static abstract class MolangOperationLeftRight extends MolangOperation{
		
		public MolangExpression leftExpr;
		public MolangExpression rightExpr;
		
		public MolangOperationLeftRight(MolangExpression leftExpr, MolangExpression rightExpr) {
			this.leftExpr = leftExpr;
			this.rightExpr = rightExpr;
		}
		
	}
	
	public static class MolangInvert extends MolangOperationRight{
		
		public MolangInvert(MolangExpression expr) {
			super(expr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(!rightExpr.eval(context).asBoolean(context));
		}
		
		@Override
		public int getPriority() {
			return 14;
		}
		
	}
	
	public static class MolangOr extends MolangOperationLeftRight{
		
		public MolangOr(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asBoolean(context) || 
									rightExpr.eval(context).asBoolean(context));
		}
		
		@Override
		public int getPriority() {
			return 4;
		}
		
	}
	
	public static class MolangAnd extends MolangOperationLeftRight{
		
		public MolangAnd(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asBoolean(context) && 
									rightExpr.eval(context).asBoolean(context));
		}
		
		@Override
		public int getPriority() {
			return 5;
		}
		
	}
	
	public static class MolangLessThan extends MolangOperationLeftRight{
		
		public MolangLessThan(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) < 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 10;
		}
		
	}
	
	public static class MolangLessThanOrEqual extends MolangOperationLeftRight{
		
		public MolangLessThanOrEqual(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) <= 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 10;
		}
		
	}
	
	public static class MolangGreaterThan extends MolangOperationLeftRight{
		
		public MolangGreaterThan(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) > 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 10;
		}
		
	}
	
	public static class MolangGreaterThanOrEqual extends MolangOperationLeftRight{
		
		public MolangGreaterThanOrEqual(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) >= 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 10;
		}
		
	}
	
	public static class MolangEqual extends MolangOperationLeftRight{
		
		public MolangEqual(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).equal(context, rightExpr.eval(context)));
		}
		
		@Override
		public int getPriority() {
			return 9;
		}
		
	}
	
	public static class MolangNotEqual extends MolangEqual{
		
		
		public MolangNotEqual(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(!super.eval(context).asBoolean(context));
		}
		
		@Override
		public int getPriority() {
			return 9;
		}
		
	}
	
	public static class MolangAdd extends MolangOperationLeftRight{
		
		public MolangAdd(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(leftExpr == null)
				return new MolangValue(rightExpr.eval(context).asNumber(context));
			return new MolangValue(leftExpr.eval(context).asNumber(context) + 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 12;
		}
		
	}
	
	public static class MolangSubtract extends MolangOperationLeftRight{
		
		public MolangSubtract(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(leftExpr == null)
				return new MolangValue(-rightExpr.eval(context).asNumber(context));
			return new MolangValue(leftExpr.eval(context).asNumber(context) - 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 12;
		}
		
	}
	
	public static class MolangMultiply extends MolangOperationLeftRight{
		
		public MolangMultiply(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) * 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 13;
		}
		
	}
	
	public static class MolangDivide extends MolangOperationLeftRight{
		
		public MolangDivide(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			return new MolangValue(leftExpr.eval(context).asNumber(context) / 
									rightExpr.eval(context).asNumber(context));
		}
		
		@Override
		public int getPriority() {
			return 13;
		}
		
	}
	
	public static class MolangGlobal extends MolangOperation{
		
		public String name;
		
		public MolangGlobal(String name) {
			this.name = name;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue val = context.getGlobal(name);
			if(val == null)
				throw new RuntimeException("No global with the name " + name);
			return val;
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangAccessField extends MolangOperationLeft{
		
		public String fieldName;
		
		public MolangAccessField(MolangExpression objExpr, String fieldName) {
			super(objExpr);
			this.fieldName = fieldName;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue value = this.leftExpr.eval(context).getField(context, fieldName);
			if(value == null)
				return new MolangValue(new MolangNull());
			return value;
		}
		
		@Override
		public int getPriority() {
			return 15;
		}
		
	}
	
	public static class MolangAccessArray extends MolangOperationLeft{
		
		public MolangExpression indexExpr;
		
		public MolangAccessArray(MolangExpression objExpr, MolangExpression indexExpr) {
			super(objExpr);
			this.indexExpr = indexExpr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue indexVal = indexExpr.eval(context);
			String indexStr = "[" + ((int)indexVal.asNumber(context)) + "]";
			MolangValue value = leftExpr.eval(context).getField(context, indexStr);
			if(value == null)
				return new MolangValue(new MolangNull());
			return value;
		}
		
		@Override
		public int getPriority() {
			return 15;
		}
		
	}
	
	public static class MolangNullCoalescing extends MolangOperationLeftRight{
		
		public MolangNullCoalescing(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue leftVal = leftExpr.eval(context);
			if(leftVal.isNull())
				return rightExpr.eval(context);
			return leftVal;
		}
		
		@Override
		public int getPriority() {
			return 3;
		}
		
	}
	
	public static class MolangBreak extends MolangOperation{
		
		@Override
		public MolangValue eval(MolangContext context) {
			context.setBreakFlag(true);
			return new MolangValue(new MolangNull());
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangContinue extends MolangOperation{
		
		@Override
		public MolangValue eval(MolangContext context) {
			context.setContinueFlag(true);
			return new MolangValue(new MolangNull());
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangReturn extends MolangOperation{
		
		public MolangExpression returnExpr;
		
		public MolangReturn(MolangExpression returnExpr) {
			this.returnExpr = returnExpr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			context.setReturnValue(returnExpr.eval(context));
			return new MolangValue(new MolangNull());
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangAssign extends MolangOperationLeftRight{
		
		public MolangAssign(MolangExpression leftExpr, MolangExpression rightExpr) {
			super(leftExpr, rightExpr);
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue newValue = rightExpr.eval(context);
			leftExpr.eval(context).set(newValue);
			return newValue;
		}
		
		@Override
		public int getPriority() {
			return 2;
		}
		
	}
	
	public static class MolangLoop extends MolangOperation{
		
		public MolangExpression countExpr;
		public MolangExpression expr;
		
		public MolangLoop(MolangExpression countExpr, MolangExpression expr) {
			this.countExpr = countExpr;
			this.expr = expr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			int count = (int) countExpr.eval(context).asNumber(context);
			context.startScope();
			context.setBreakFlag(false);
			context.setContinueFlag(false);
			for(int i = 0; i < count; ++i) {
				expr.eval(context);
				if(context.getBreakFlag() || context.getReturnFlag())
					break;
			}
			context.endScope();
			return new MolangValue(new MolangNull());
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangForEach extends MolangOperation{
		
		public MolangExpression tmpExpr;
		public MolangExpression arrayExpr;
		public MolangExpression expr;
		
		public MolangForEach(MolangExpression tmpExpr, MolangExpression arrayExpr, MolangExpression expr) {
			this.tmpExpr = tmpExpr;
			this.arrayExpr = arrayExpr;
			this.expr = expr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue tmpValue = tmpExpr.eval(context);
			MolangValue arrayValue = arrayExpr.eval(context);
			if(!(arrayValue.getImpl() instanceof MolangArray))
				throw new RuntimeException("Array value is not an array");
			
			MolangArray array = (MolangArray) arrayValue.getImpl();
			context.startScope();
			context.setBreakFlag(false);
			context.setContinueFlag(false);
			for(int i = 0; i < array.get().size(); ++i) {
				tmpValue.set(array.get().get(i));
				
				expr.eval(context);
				if(context.getBreakFlag() || context.getReturnFlag())
					break;
			}
			context.endScope();
			return new MolangValue(new MolangNull());
		}
		
		@Override
		public int getPriority() {
			return 16;
		}
		
	}
	
	public static class MolangConditional extends MolangOperationLeft{
		
		public MolangExpression falseExpr;
		public MolangExpression trueExpr;
		
		public MolangConditional(MolangExpression expr, MolangExpression trueExpr, MolangExpression falseExpr) {
			super(expr);
			this.falseExpr = falseExpr;
			this.trueExpr = trueExpr;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			if(leftExpr.eval(context).asBoolean(context))
				return trueExpr.eval(context);
			return falseExpr.eval(context);
		}
		
		@Override
		public int getPriority() {
			return 3;
		}
		
	}
	
	public static class MolangCall extends MolangOperationLeft{
		
		public List<MolangExpression> argumentExprs;
		
		public MolangCall(MolangExpression functionExpr, List<MolangExpression> argumentExprs) {
			super(functionExpr);
			this.argumentExprs = argumentExprs;
		}
		
		@Override
		public MolangValue eval(MolangContext context) {
			MolangValue func = leftExpr.eval(context);
			if(func.getImpl() instanceof MolangFunction) {
				List<MolangValue> arguments = new ArrayList<MolangValue>();
				for(MolangExpression argExpr : argumentExprs)
					arguments.add(argExpr.eval(context));
				return func.call(context, arguments);
			}
			return func;
		}
		
		@Override
		public int getPriority() {
			return 15;
		}
		
	}
	
	public abstract MolangValue eval(MolangContext context);
	public abstract int getPriority();
	
}
