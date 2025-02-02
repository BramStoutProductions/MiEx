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

package nl.bramstout.mcworldexporter.molang;

import java.util.ArrayList;
import java.util.List;

import nl.bramstout.mcworldexporter.molang.MolangExpression.MolangConstantExpression;
import nl.bramstout.mcworldexporter.molang.MolangExpression.MolangOperationExpression;
import nl.bramstout.mcworldexporter.molang.MolangExpression.MolangParenthesesExpression;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangAccessArray;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangAccessField;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangAdd;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangAnd;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangAssign;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangBreak;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangCall;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangConditional;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangContinue;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangDivide;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangEqual;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangForEach;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangGlobal;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangGreaterThan;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangGreaterThanOrEqual;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangInvert;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangLessThan;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangLessThanOrEqual;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangLoop;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangMultiply;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangNotEqual;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangNullCoalescing;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangOperationLeft;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangOperationLeftRight;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangOperationRight;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangOr;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangReturn;
import nl.bramstout.mcworldexporter.molang.MolangOperation.MolangSubtract;

public class MolangParser {

	public static String[] globalNames = new String[] {
			"this", "temp", "math", "query", "variable", "c", "q", "t", "v",
			"Geometry", "Material", "Texture", "Array", "context"
	};
	
	private static class Reader{
		
		private String code;
		private int pos;
		
		public Reader(String code) {
			this.code = code;
			this.pos = 0;
		}
		
		public char read() {
			if(pos >= code.length())
				return 0;
			return code.charAt(pos++);
		}
		
		public char peek() {
			if(pos >= code.length())
				return 0;
			return code.charAt(pos);
		}
		
		public char peek(int offset) {
			if((pos + offset) >= code.length())
				return 0;
			return code.charAt(pos + offset);
		}
		
		public void back() {
			if(pos > 0)
				pos--;
		}
		
		public boolean is(String check) {
			if(pos >= code.length())
				return false;
			return code.substring(pos, Math.min(pos + check.length(), code.length())).equalsIgnoreCase(check);
		}
		
		/**
		 * Reads until it finds a non-whitespace character.
		 */
		public void seekNextChar() {
			for(; pos < code.length(); ++pos) {
				char c = code.charAt(pos);
				if(c == 0 || !Character.isWhitespace(c))
					return;
			}
		}
		
	}
	
	public static MolangScript parse(String code) {
		return parseMultiline(new Reader(code));
	}
	
	private static MolangScript parseMultiline(Reader reader) {
		List<MolangExpression> expressions = new ArrayList<MolangExpression>();
		
		while(true) {
			reader.seekNextChar();
			char c = reader.read();
			if(c == 0 || c == '}' || c == ')' || c == ']') 
				break;
			if(c == ';')
				continue;
			
			reader.back();
			expressions.add(parseExpression(reader));
		}
		
		MolangScript script = new MolangScript(expressions);
		script.setOriginalCode(reader.code);
		return script;
	}
	
	private static MolangExpression parseExpression(Reader reader) {
		int startPos = reader.pos;
		List<MolangExpression> tokens = new ArrayList<MolangExpression>();
		
		MolangExpression prevToken = null;
		while(true) {
			reader.seekNextChar();
			char c = reader.peek();

			if(c == 0 || c == ';' || c == '}' || c == ')' || c == ']' || c == ':' || c == ',') {
				break;
			}
			
			prevToken = parseToken(reader, prevToken);
			tokens.add(prevToken);
		}
		
		int maxPriority = -1;
		for(MolangExpression token : tokens)
			if(token instanceof MolangOperationExpression)
				maxPriority = Math.max(maxPriority, ((MolangOperationExpression) token).getOperation().getPriority());
		
		for(; maxPriority >= 0; --maxPriority) {
			// We need to combine the tokens into a single MolangExpression
			List<MolangExpression> newTokens = new ArrayList<MolangExpression>();
			
			// Go through and combine tokens of maxPriority together
			for(int i = 0; i < tokens.size(); ++i) {
				MolangExpression token = tokens.get(i);
				if(!(token instanceof MolangOperationExpression)) {
					newTokens.add(token);
					continue;
				}
				MolangOperationExpression opToken = (MolangOperationExpression) token;
				if(opToken.getOperation().getPriority() != maxPriority) {
					newTokens.add(token);
					continue;
				}
				
				// opToken is of this priority, so let's combine it with another
				// token.
				if(opToken.getOperation() instanceof MolangOperationLeft) {
					if(((MolangOperationLeft)opToken.getOperation()).leftExpr == null)
						((MolangOperationLeft)opToken.getOperation()).leftExpr = newTokens.remove(newTokens.size()-1);
					newTokens.add(token);
					continue;
				}
				if(opToken.getOperation() instanceof MolangOperationRight) {
					if(((MolangOperationRight)opToken.getOperation()).rightExpr == null) {
						((MolangOperationRight)opToken.getOperation()).rightExpr = tokens.get(i+1);
						i++;
					}
					newTokens.add(token);
					continue;
				}
				if(opToken.getOperation() instanceof MolangOperationLeftRight) {
					if(((MolangOperationLeftRight)opToken.getOperation()).leftExpr == null) {
						if(newTokens.size() > 0) {
							MolangExpression leftToken = newTokens.get(newTokens.size()-1);
							boolean merge = true;
							if(leftToken instanceof MolangOperationExpression) {
								MolangOperationExpression leftOpToken = (MolangOperationExpression) leftToken;
								if(leftOpToken.getOperation() instanceof MolangOperationLeftRight) {
									if(((MolangOperationLeftRight)leftOpToken.getOperation()).rightExpr == null) {
										merge = false;
									}
								}
							}
							if(merge)
								((MolangOperationLeftRight)opToken.getOperation()).leftExpr = newTokens.remove(newTokens.size()-1);
						}else
							((MolangOperationLeftRight)opToken.getOperation()).leftExpr = new MolangConstantExpression(new MolangValue(0));
					}
					if(((MolangOperationLeftRight)opToken.getOperation()).rightExpr == null) {
						MolangExpression rightToken = tokens.get(i+1);
						if(rightToken instanceof MolangOperationExpression) {
							MolangOperationExpression rightOpToken = (MolangOperationExpression) rightToken;
							if(rightOpToken.getOperation() instanceof MolangOperationLeftRight) {
								if(((MolangOperationLeftRight)rightOpToken.getOperation()).rightExpr == null) {
									((MolangOperationLeftRight)rightOpToken.getOperation()).rightExpr = tokens.get(i+2);
									i++;
								}
							}
						}
						((MolangOperationLeftRight)opToken.getOperation()).rightExpr = rightToken;
						i++;
					}
					newTokens.add(token);
					continue;
				}
				// Operation is one that doesn't need to be combined.
				newTokens.add(token);
			}
			tokens = newTokens;
		}
		// Hopefully we only have a single expression now
		if(tokens.size() > 1)
			throw new RuntimeException("Invalid expression: " + reader.code.substring(startPos, reader.pos));
		if(tokens.size() <= 0)
			return new MolangConstantExpression(new MolangValue());
		return tokens.get(0);
	}
	
	private static MolangExpression parseToken(Reader reader, MolangExpression prevToken) {
		char c = reader.peek();
		// Let's see what kind of expression this is
		
		if(Character.isDigit(c)) {
			// Number literal
			return parseNumberLiteral(reader);
		}
		if(c == '\'') {
			// String literal
			return parseStringLiteral(reader);
		}
		if(reader.is("true")) {
			reader.pos += "true".length();
			return new MolangConstantExpression(new MolangValue(true));
		}
		if(reader.is("false")) {
			reader.pos += "false".length();
			return new MolangConstantExpression(new MolangValue(false));
		}
		if(c == '(') {
			if(prevToken instanceof MolangOperationExpression) {
				// If in the previous token we are accessing some value,
				// this would be a call, so check for that.
				MolangOperation op = ((MolangOperationExpression)prevToken).getOperation();
				if(op instanceof MolangOperation.MolangAccessArray ||
						op instanceof MolangOperation.MolangAccessField ||
						op instanceof MolangOperation.MolangCall ||
						op instanceof MolangOperation.MolangConditional ||
						op instanceof MolangOperation.MolangGlobal) {
					return parseCall(reader);
				}
			} else if(prevToken instanceof MolangParenthesesExpression) {
				// Parentheses directly after a set of parentheses
				// indicates that this is supposed to be a function call.
				return parseCall(reader);
			}
			return parseParentheses(reader);
		}
		if(c == '{') {
			reader.read();
			return parseMultiline(reader);
		}
		if(reader.is("return ")) {
			if(reader.is("return ") || reader.is("return;") || reader.is("return)") || reader.is("return}"))
				return parseReturn(reader);
		}
		if(reader.is("break")) {
			if(reader.is("break ") || reader.is("break;") || reader.is("break)") || reader.is("break}"))
				return parseBreak(reader);
		}
		if(reader.is("continue")) {
			if(reader.is("continue ") || reader.is("continue;") || reader.is("continue)") || reader.is("continue}"))
				return parseContinue(reader);
		}
		if(reader.is("loop")) {
			int prevPos = reader.pos;
			reader.pos += 4;
			reader.seekNextChar();
			char c2 = reader.peek();
			reader.pos = prevPos;
			if(c2 == '(')
				return parseLoop(reader);
		}
		if(reader.is("for_each")) {
			int prevPos = reader.pos;
			reader.pos += 4;
			reader.seekNextChar();
			char c2 = reader.peek();
			reader.pos = prevPos;
			if(c2 == '(')
				return parseForEach(reader);
		}
		if(c == '.') {
			return parseAccessField(reader);
		}
		if(c == '[') {
			return parseAccessArray(reader);
		}
		if(reader.is("??")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangNullCoalescing(null, null));
		}
		if(c == '?') {
			return parseConditional(reader);
		}
		if(reader.is("!=")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangNotEqual(null, null));
		}
		if(c == '!') {
			reader.read();
			return new MolangOperationExpression(new MolangInvert(null));
		}
		if(reader.is("||")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangOr(null, null));
		}
		if(reader.is("&&")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangAnd(null, null));
		}
		if(reader.is("<=")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangLessThanOrEqual(null, null));
		}
		if(reader.is(">=")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangGreaterThanOrEqual(null, null));
		}
		if(reader.is("==")) {
			reader.read();
			reader.read();
			return new MolangOperationExpression(new MolangEqual(null, null));
		}
		if(c == '<') {
			reader.read();
			return new MolangOperationExpression(new MolangLessThan(null, null));
		}
		if(c == '>') {
			reader.read();
			return new MolangOperationExpression(new MolangGreaterThan(null, null));
		}
		if(c == '+') {
			reader.read();
			return new MolangOperationExpression(new MolangAdd(null, null));
		}
		if(c == '-') {
			reader.read();
			return new MolangOperationExpression(new MolangSubtract(null, null));
		}
		if(c == '*') {
			reader.read();
			return new MolangOperationExpression(new MolangMultiply(null, null));
		}
		if(c == '/') {
			reader.read();
			return new MolangOperationExpression(new MolangDivide(null, null));
		}
		if(c == '=') {
			reader.read();
			return new MolangOperationExpression(new MolangAssign(null, null));
		}
		if(Character.isAlphabetic(c)) {
			for(String globalName : globalNames) {
				if(reader.is(globalName)) {
					if(!Character.isAlphabetic(reader.peek(globalName.length()))) {
						return parseGlobal(reader);
					}
				}
			}
			return parseUnEscapredStringLiteral(reader);
		}
		throw new RuntimeException("Syntax Error: Unknown character at " + reader.pos + " = " + c);
	}
	
	private static MolangExpression parseNumberLiteral(Reader reader) {
		int startPos = reader.pos;
		int endPos = reader.pos;
		char prevC = 0;
		while(true) {
			char c = reader.read();
			if(c == 0) {
				endPos = reader.pos;
				break;
			}
			if(c != '.' && !Character.isDigit(c) && c != 'f' && c != 'e' && !((c == '-' || c == '+') && (prevC == 'e' || prevC == 0))) {
				reader.back();
				endPos = reader.pos;
				break;
			}
			prevC = c;
		}
		String numberStr = reader.code.substring(startPos, endPos);
		return new MolangConstantExpression(new MolangValue(Float.parseFloat(numberStr)));
	}
	
	private static MolangExpression parseStringLiteral(Reader reader) {
		reader.read();
		int startPos = reader.pos;
		int endPos = reader.pos;
		while(true) {
			char c = reader.read();
			if(c == 0) {
				endPos = reader.pos;
				break;
			}
			if(c == '\'') {
				endPos = reader.pos - 1;
				break;
			}
		}
		String str = reader.code.substring(startPos, endPos);
		return new MolangConstantExpression(new MolangValue(str));
	}
	
	private static MolangExpression parseUnEscapredStringLiteral(Reader reader) {
		reader.read();
		int startPos = reader.pos;
		int endPos = reader.pos;
		while(true) {
			char c = reader.read();
			if(c == 0) {
				endPos = reader.pos;
				break;
			}
			if(c == ';' || c == ')' || c == '}' || c == ',' || c == '.') {
				endPos = reader.pos - 1;
				break;
			}
		}
		String str = reader.code.substring(startPos, endPos);
		return new MolangConstantExpression(new MolangValue(str));
	}
	
	private static MolangExpression parseParentheses(Reader reader) {
		reader.read(); // Opening parentheses
		MolangExpression expr = new MolangParenthesesExpression(parseExpression(reader));
		reader.read(); // Closing parentheses
		return expr;
	}
	
	private static MolangExpression parseReturn(Reader reader) {
		reader.pos += "return".length();
		reader.seekNextChar();
		char c = reader.peek();
		if(c == 0 || c == ';' || c == ')' || c == '}') {
			// No return value
			return new MolangOperationExpression(new MolangReturn(new MolangConstantExpression(new MolangValue())));
		}
		MolangExpression expr = new MolangOperationExpression(new MolangReturn(parseExpression(reader)));
		reader.read(); // ';'
		return expr;
	}
	
	private static MolangExpression parseBreak(Reader reader) {
		reader.pos += "break".length();
		return new MolangOperationExpression(new MolangBreak());
	}
	
	private static MolangExpression parseContinue(Reader reader) {
		reader.pos += "continue".length();
		return new MolangOperationExpression(new MolangContinue());
	}
	
	private static MolangExpression parseLoop(Reader reader) {
		reader.pos += "loop".length();
		reader.seekNextChar();
		reader.read(); // '(' char
		List<MolangExpression> arguments = new ArrayList<MolangExpression>();
		while(true) {
			reader.seekNextChar();
			char c = reader.read();
			if(c == 0 || c == ')') {
				break;
			}
			if(c == ';')
				continue;
			reader.back();
			arguments.add(parseExpression(reader));
		}
		if(arguments.size() <= 1)
			throw new RuntimeException("Syntax error: Invalid loop at " + reader.pos);
		return new MolangOperationExpression(new MolangLoop(arguments.get(0), arguments.get(1)));
	}
	
	private static MolangExpression parseForEach(Reader reader) {
		reader.pos += "for_each".length();
		reader.seekNextChar();
		reader.read(); // '(' char
		List<MolangExpression> arguments = new ArrayList<MolangExpression>();
		while(true) {
			reader.seekNextChar();
			char c = reader.read();
			if(c == 0 || c == ')') {
				break;
			}
			if(c == ';')
				continue;
			reader.back();
			arguments.add(parseExpression(reader));
		}
		if(arguments.size() <= 2)
			throw new RuntimeException("Syntax error: Invalid for_each at " + reader.pos);
		return new MolangOperationExpression(new MolangForEach(arguments.get(0), arguments.get(1), arguments.get(2)));
	}
	
	private static MolangExpression parseCall(Reader reader) {
		reader.read();
		List<MolangExpression> arguments = new ArrayList<MolangExpression>();
		while(true) {
			char c = reader.read();
			if(c == 0 || c == ')') {
				break;
			}
			reader.back();
			arguments.add(parseExpression(reader));
			if(reader.peek() == ',')
				reader.read();
		}
		return new MolangOperationExpression(new MolangCall(null, arguments));
	}
	
	private static MolangExpression parseAccessField(Reader reader) {
		reader.read();
		int startPos = reader.pos;
		int endPos = reader.pos;
		while(true) {
			char c = reader.read();
			if(c == 0) {
				endPos = reader.pos;
				break;
			}
			if(!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
				reader.back();
				endPos = reader.pos;
				break;
			}
		}
		String fieldName = reader.code.substring(startPos, endPos);
		return new MolangOperationExpression(new MolangAccessField(null, fieldName.toLowerCase()));
	}
	
	private static MolangExpression parseAccessArray(Reader reader) {
		reader.read();
		MolangExpression indexExpr = parseExpression(reader);
		return new MolangOperationExpression(new MolangAccessArray(null, indexExpr));
	}
	
	private static MolangExpression parseConditional(Reader reader) {
		reader.read();
		MolangExpression trueExpr = parseExpression(reader);
		reader.seekNextChar();
		char c = reader.read();
		MolangExpression falseExpr = null;
		if(c != ':') {
			reader.back();
			falseExpr = new MolangConstantExpression(new MolangValue());
		}else {
			falseExpr = parseExpression(reader);
		}
		return new MolangOperationExpression(new MolangConditional(null, trueExpr, falseExpr));
	}
	
	private static MolangExpression parseGlobal(Reader reader) {
		int startPos = reader.pos;
		int endPos = reader.pos;
		while(true) {
			char c = reader.read();
			if(c == 0) {
				endPos = reader.pos;
				break;
			}
			if(!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
				reader.back();
				endPos = reader.pos;
				break;
			}
		}
		String globalName = reader.code.substring(startPos, endPos);
		return new MolangOperationExpression(new MolangGlobal(globalName.toLowerCase()));
	}
	
}
