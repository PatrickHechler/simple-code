// Generated from de\hechler\patrick\codesprachen\simple\antlr4\SimpleCodeGrammar.g4 by ANTLR 4.3
package de.hechler.patrick.codesprachen.simple.antlr4;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SimpleCodeGrammarParser}.
 */
public interface SimpleCodeGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SimpleCodeGrammarParser#r}.
	 * @param ctx the parse tree
	 */
	void enterR(@NotNull SimpleCodeGrammarParser.RContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleCodeGrammarParser#r}.
	 * @param ctx the parse tree
	 */
	void exitR(@NotNull SimpleCodeGrammarParser.RContext ctx);
}