/*
 * Copyright 2015, Hridesh Rajan, Robert Dyer, Hoan Nguyen
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boa.datagen.util;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

//import org.eclipse.wst.jsdt.core.dom.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.*;

import boa.types.Ast.Declaration;
import boa.types.Ast.Expression;
import boa.types.Ast.Method;
import boa.types.Ast.Namespace;
import boa.types.Ast.PositionInfo;
import boa.types.Ast.Statement.StatementKind;
import boa.types.Ast.Type;
import boa.types.Ast.TypeKind;
import boa.types.Ast.Variable;

/**
 * @author rdyer
 */
public class JavaScriptVisitor implements NodeVisitor{
	private HashMap<String, Integer> nameIndices;

	private AstRoot root = null;
	private PositionInfo.Builder pos = null;
	private String src = null;
	private Namespace.Builder b = Namespace.newBuilder();
	private List<boa.types.Ast.Comment> comments = new ArrayList<boa.types.Ast.Comment>();
	private List<String> imports = new ArrayList<String>();
	private Stack<List<boa.types.Ast.Declaration>> declarations = new Stack<List<boa.types.Ast.Declaration>>();
	private Stack<boa.types.Ast.Modifier> modifiers = new Stack<boa.types.Ast.Modifier>();
	private Stack<boa.types.Ast.Expression> expressions = new Stack<boa.types.Ast.Expression>();
	private Stack<List<boa.types.Ast.Variable>> fields = new Stack<List<boa.types.Ast.Variable>>();
	private Stack<List<boa.types.Ast.Method>> methods = new Stack<List<boa.types.Ast.Method>>();
	private Stack<List<boa.types.Ast.Statement>> statements = new Stack<List<boa.types.Ast.Statement>>();

	public JavaScriptVisitor(String src) {
		super();
		this.src = src;
	}

	public Namespace getNamespaces(AstRoot node) {
		root = node;
		node.visit(this);
		return b.build();
	}

	public List<boa.types.Ast.Comment> getComments() {
		return comments;
	}

	public List<String> getImports() {
		return imports;
	}

	/*
	 * public void preVisit(ASTNode node) { buildPosition(node); }
	 */

	private void buildPosition(final AstNode node) {
		pos = PositionInfo.newBuilder();
		int start = node.getPosition();// getStartPosition();
		int length = node.getLength() ;//getLength();
		pos.setStartPos(start);
		pos.setLength(length);
		pos.setStartLine(root.getBaseLineno());
		//FIXME pos.setStartCol(root.  getColumnNumber(start));
		pos.setEndLine(root.getEndLineno());
		//FIXME pos.setEndCol(root.getColumnNumber(start + length));
	}

	public boolean visit(AstRoot node) {
		Name pkg = (Name)node.getFirstChild(); // getPackage();
		if (pkg == null) {
			b.setName("");
		} else {
			b.setName(pkg.getIdentifier());//  .getFullyQualifiedName());
		}
		for (Object i : node.gets imports()) {
			ImportDeclaration id = (ImportDeclaration) i;
			String imp = "";
			if (id.isStatic())
				imp += "static ";
			imp += id.getName().getFullyQualifiedName();
			if (id.isOnDemand())
				imp += ".*";
			imports.add(imp);
		}
		for (Object t : node.types()) {
			declarations.push(new ArrayList<boa.types.Ast.Declaration>());
			((AbstractTypeDeclaration) t).accept(this);
			for (boa.types.Ast.Declaration d : declarations.pop())
				b.addDeclarations(d);
		}
		for (Object c : node.getComments())
				((Comment) c).visit(this);

		if (!node.getStatements().isEmpty()) {
			for (Object s : node.getStatements()) {
				if (s instanceof FunctionNode) {
					methods.push(new ArrayList<boa.types.Ast.Method>());
					((FunctionNode) s).visit(this);
					for (boa.types.Ast.Method m : methods.pop())
						b.addMethods(m);
				} else {
					statements.push(new ArrayList<boa.types.Ast.Statement>());
					((AstNode) s).visit(this);
					for (boa.types.Ast.Statement d : statements.pop())
						b.addStatements(d);
				}
			}
		}
		return false;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		boa.types.Ast.Declaration.Builder b = boa.types.Ast.Declaration.newBuilder();
		b.setName("");
		b.setKind(boa.types.Ast.TypeKind.ANONYMOUS);
		for (Object d : node.bodyDeclarations()) {
			if (d instanceof FieldDeclaration) {
				fields.push(new ArrayList<boa.types.Ast.Variable>());
				((FieldDeclaration) d).accept(this);
				for (boa.types.Ast.Variable v : fields.pop())
					b.addFields(v);
			} else if (d instanceof FunctionDeclaration) {
				methods.push(new ArrayList<boa.types.Ast.Method>());
				((FunctionDeclaration) d).accept(this);
				for (boa.types.Ast.Method m : methods.pop())
					b.addMethods(m);
			} else if (d instanceof Initializer) {
				methods.push(new ArrayList<boa.types.Ast.Method>());
				((Initializer) d).accept(this);
				for (boa.types.Ast.Method m : methods.pop())
					b.addMethods(m);
			} else {
				declarations.push(new ArrayList<boa.types.Ast.Declaration>());
				((BodyDeclaration) d).accept(this);
				for (boa.types.Ast.Declaration nd : declarations.pop())
					b.addNestedDeclarations(nd);
			}
		}
		declarations.peek().add(b.build());
		return false;
	}

	public boolean visit(Comment node) {
		boa.types.Ast.Comment.Builder b = boa.types.Ast.Comment.newBuilder();
		buildPosition(node);
		b.setPosition(pos.build());
		if (node.getCommentType() == Token.CommentType.BLOCK_COMMENT){
			b.setKind(boa.types.Ast.Comment.CommentKind.BLOCK);
		}else if (node.getCommentType() == Token.CommentType.LINE){
			b.setKind(boa.types.Ast.Comment.CommentKind.LINE);
		}else if (node.getCommentType() == Token.CommentType.JSDOC){
			b.setKind(boa.types.Ast.Comment.CommentKind.DOC);
		}
		b.setValue(src.substring(node.getLineno(), node.getLineno() + node.getLength()));
		comments.add(b.build());
		return false;
	}

	//////////////////////////////////////////////////////////////
	// Type Declarations

	@Override
	public boolean visit(TypeDeclaration node) {
		boa.types.Ast.Declaration.Builder b = boa.types.Ast.Declaration.newBuilder();
		b.setName(node.getName().getFullyQualifiedName());
		b.setKind(boa.types.Ast.TypeKind.CLASS);
		for (Object m : node.modifiers()) {
			((AstNode) m).visit(this);
			b.addModifiers(modifiers.pop());
		}

		if (node.getSuperclassType() != null) {
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			tb.setName(typeName(node.getSuperclassType()));
			tb.setKind(boa.types.Ast.TypeKind.CLASS);
			b.addParents(tb.build());
		}

		for (Object d : node.bodyDeclarations()) {
			if (d instanceof FieldDeclaration) {
				fields.push(new ArrayList<boa.types.Ast.Variable>());
				((FieldDeclaration) d).accept(this);
				for (boa.types.Ast.Variable v : fields.pop())
					b.addFields(v);
			} else if (d instanceof Initializer) {
				methods.push(new ArrayList<boa.types.Ast.Method>());
				((Initializer) d).accept(this);
				for (boa.types.Ast.Method m : methods.pop())
					b.addMethods(m);
			} else if (d instanceof FunctionDeclaration) {
				methods.push(new ArrayList<boa.types.Ast.Method>());
				((FunctionDeclaration) d).accept(this);
				for (boa.types.Ast.Method m : methods.pop())
					b.addMethods(m);
			} else {
				declarations.push(new ArrayList<boa.types.Ast.Declaration>());
				((BodyDeclaration) d).accept(this);
				for (boa.types.Ast.Declaration nd : declarations.pop())
					b.addNestedDeclarations(nd);
			}
		}
		declarations.peek().add(b.build());
		return false;
	}

	//////////////////////////////////////////////////////////////
	// Field/Method Declarations

	public boolean visit(VariableDeclaration node) {
		List<boa.types.Ast.Variable> list = fields.peek();
		for (Object o : node.getVariables()) {
			VariableInitializer f = (VariableInitializer) o;
			Variable.Builder b = Variable.newBuilder();
			b.setName(Token.typeToName(f.getType()));//getName().getFullyQualifiedName());
			if(modifiers.peek() != null)
				b.addModifiers(modifiers.pop());
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			String name = (Token.typeToName(node.getType()));//typeName(node.getType());
			tb.setName(name);
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			b.setVariableType(tb.build());
			if (f.getInitializer() != null) {
				f.getInitializer().visit(this);
				b.setInitializer(expressions.pop());
			}
			list.add(b.build());
		}
		return false;
	}
	
	public boolean visit(LetNode node){
		List<boa.types.Ast.Variable> list = fields.peek();
		for (Object o : node.getVariables()) {
			VariableInitializer f = (VariableInitializer) o;
			Variable.Builder b = Variable.newBuilder();
			b.setName(Token.typeToName(f.getType()));//getName().getFullyQualifiedName());
			if(modifiers.peek() != null)
				b.addModifiers(modifiers.pop());
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			String name = (Token.typeToName(node.getType()));//typeName(node.getType());
			tb.setName(name);
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			b.setVariableType(tb.build());
			if (f.getInitializer() != null) {
				f.getInitializer().visit(this);
				b.setInitializer(expressions.pop());
			}
			list.add(b.build());
		}
		return false;
	}

	//////////////////////////////////////////////////////////////
	// Modifiers and Annotations
	public boolean visit(Modifier node) {
		boa.types.Ast.Modifier.Builder b = boa.types.Ast.Modifier.newBuilder();
		if (node.isFinal())
			b.setKind(boa.types.Ast.Modifier.ModifierKind.FINAL);
		else if (node.isAbstract())
			b.setKind(boa.types.Ast.Modifier.ModifierKind.ABSTRACT);
		else if (node.isStatic())
			b.setKind(boa.types.Ast.Modifier.ModifierKind.STATIC);
		else if (node.isSynchronized())
			b.setKind(boa.types.Ast.Modifier.ModifierKind.SYNCHRONIZED);
		else if (node.isPublic()) {
			b.setKind(boa.types.Ast.Modifier.ModifierKind.VISIBILITY);
			b.setVisibility(boa.types.Ast.Modifier.Visibility.PUBLIC);
		} else if (node.isPrivate()) {
			b.setKind(boa.types.Ast.Modifier.ModifierKind.VISIBILITY);
			b.setVisibility(boa.types.Ast.Modifier.Visibility.PRIVATE);
		} else if (node.isProtected()) {
			b.setKind(boa.types.Ast.Modifier.ModifierKind.VISIBILITY);
			b.setVisibility(boa.types.Ast.Modifier.Visibility.PROTECTED);
		} else {
			b.setKind(boa.types.Ast.Modifier.ModifierKind.OTHER);
			b.setOther(node.getKeyword().toString());
		}
		modifiers.push(b.build());
		return false;
	}

	//////////////////////////////////////////////////////////////
	// Statements

	public boolean visit(Block node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.BLOCK);
		for (Node s: node){
			if (s.getType() == Token.FUNCTION) {
				methods.push(new ArrayList<boa.types.Ast.Method>());
				((AstNode) s).visit(this);
				b.setKind(StatementKind.OTHER);
			} else {
				statements.push(new ArrayList<boa.types.Ast.Statement>());
				((AstNode) s).visit(this);
				for (boa.types.Ast.Statement st : statements.pop())
					b.addStatements(st);
			}
			s = s.getNext();
		}
		list.add(b.build());
		return false;
	}

	public boolean visit(BreakStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.BREAK);
		if (node.getBreakLabel() != null) {
			boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
			eb.setLiteral(node.getBreakLabel().getIdentifier()); //  getFullyQualifiedName());
			eb.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
			b.setExpression(eb.build());
		}
		list.add(b.build());
		return false;
	}
	
	public boolean visit(CatchClause node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.CATCH);
		AstNode ex = node.getCatchCondition() ;
		Variable.Builder vb = Variable.newBuilder();
		vb.setName(Token.typeToName(ex.getType()));
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		String name = Token.typeToName(ex.getType());
		tb.setName(name);
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		vb.setVariableType(tb.build());
		b.setVariableDeclaration(vb.build());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		for (Object s : node.getBody())
			((AstNode) s).visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	
	public boolean visit(ConstructorInvocation node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.EXPRESSION);
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.METHODCALL);
		eb.setMethod("<init>");
		for (Object a : node.arguments()) {
			((org.eclipse.wst.jsdt.core.dom.Statement) a).accept(this);
			eb.addMethodArgs(expressions.pop());
		}
		for (Object t : node.typeArguments()) {
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			tb.setName(typeName((org.eclipse.wst.jsdt.core.dom.Type) t));
			tb.setKind(boa.types.Ast.TypeKind.GENERIC);
			eb.addGenericParameters(tb.build());
		}
		b.setExpression(eb.build());
		list.add(b.build());
		return false;
	}

	public boolean visit(ContinueStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.CONTINUE);
		if (node.getLabel() != null) {
			boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
			
			eb.setLiteral(Token.typeToName(node.getType()));// getFullyQualifiedName());
			eb.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
			b.setExpression(eb.build());
		}
		list.add(b.build());
		return false;
	}

	
	public boolean visit(DoLoop node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.DO);
		node.getCondition().visit(this);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	public boolean visit(EmptyStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.EMPTY);
		list.add(b.build());
		return false;
	}
	
	public boolean visit(EmptyExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		Expression list = expressions.peek();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.EMPTY);
		list.add(b.build());
		return false;
	}
	
	public boolean visit(Error node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.OTHER);
		list.add(b.build());
		return false;
	}

	public boolean visit(EnhancedForStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.FOR);
		VariableDeclaration ex = node.getParameter();
		Variable.Builder vb = Variable.newBuilder();
		vb.setName(ex.getName().getFullyQualifiedName());
		for (Object m : ex.modifiers()) {
			((Modifier) m).accept(this);
			vb.addModifiers(modifiers.pop());
		}
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		String name = typeName(ex.getType());
		for (int i = 0; i < ex.getExtraDimensions(); i++)
			name += "[]";
		tb.setName(name);
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		vb.setVariableType(tb.build());
		if (ex.getInitializer() != null) {
			ex.getInitializer().accept(this);
			vb.setInitializer(expressions.pop());
		}
		b.setVariableDeclaration(vb.build());
		node.getExpression().accept(this);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().accept(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	public boolean visit(ForInLoop node) {
		boa.types.Ast.Statement.Builder s = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		s.setKind(boa.types.Ast.Statement.StatementKind.FOR);
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().visit(this);
		for (boa.types.Ast.Statement x : statements.pop())
			s.addStatements(x);

		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getIterator().visit(this);
		for (boa.types.Ast.Statement x : statements.pop())
			s.addStatements(x);

		node.getIteratedObject().visit(this);
		s.setExpression(expressions.pop());
		list.add(s.build());
		return false;
	}

	public boolean visit(FunctionCall node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.METHODCALL);
		if (node.shortName() != null)
			b.setMethod(node.shortName()); //. getFullyQualifiedName());
		if (node.getTarget()  != null) {
			node.getTarget().visit(this);
			b.addExpressions(expressions.pop());
		}

		for (Object a : node.getArguments()) {
			((AstNode) a).visit(this);
			b.addMethodArgs(expressions.pop());
		}
//		for (Object t : node.typeArguments()) {
//			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
//			tb.setName(typeName((org.eclipse.wst.jsdt.core.dom.Type) t));
//			tb.setKind(boa.types.Ast.TypeKind.GENERIC);
//			b.addGenericParameters(tb.build());
//		}
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(ListExpression node) {
		boa.types.Ast.Expression.Builder bui = boa.types.Ast.Expression.newBuilder();
		bui.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		for (Object a : node.expressions()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) a).accept(this);
			bui.addExpressions(expressions.pop());
		}
		expressions.push(bui.build());
		return false;
	}

	public boolean visit(jsdtName node) {
		boa.types.Ast.Expression.Builder bui = boa.types.Ast.Expression.newBuilder();
		bui.setVariable(node.getFullyQualifiedName());
		bui.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		((org.eclipse.wst.jsdt.core.dom.Expression) node).accept(this);
		bui.addExpressions(expressions.pop());
		expressions.push(bui.build());
		return false;
	}

	public boolean visit(ExpressionStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.EXPRESSION);
		node.getExpression().visit(this);
		b.setExpression(expressions.pop());
		list.add(b.build());
		return false;
	}

	public boolean visit(WithStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.OTHER);
		node.getExpression().visit(this);
		b.setExpression(expressions.pop());
		node.getStatement().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(TextElement node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		b.setLiteral(node.getText());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(ForStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.FOR);
		for (Object e : node.initializers()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) e).accept(this);
			b.addInitializations(expressions.pop());
		}
		for (Object e : node.updaters()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) e).accept(this);
			b.addUpdates(expressions.pop());
		}
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			b.setExpression(expressions.pop());
		}
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().accept(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	public boolean visit(IfStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.IF);
		node.getCondition().visit(this);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getThenPart().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		// FIXME
		if (node.getElsePart() != null) {
			statements.push(new ArrayList<boa.types.Ast.Statement>());
			node.getElsePart().visit(this);
			for (boa.types.Ast.Statement s : statements.pop())
				b.addStatements(s);
		}
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(Initializer node) {
		List<boa.types.Ast.Method> list = methods.peek();
		Method.Builder b = Method.newBuilder();
		b.setName("<clinit>");
		for (Object m : node.modifiers()) {
			((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
			b.addModifiers(modifiers.pop());
		}
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		tb.setName("void");
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		b.setReturnType(tb.build());
		if (node.getBody() != null) {
			statements.push(new ArrayList<boa.types.Ast.Statement>());
			node.getBody().accept(this);
			for (boa.types.Ast.Statement s : statements.pop())
				b.addStatements(s);
		}
		list.add(b.build());
		return false;
	}

	public boolean visit(LabeledStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.LABEL);
		for (Label l: node.getLabels()){
			statements.push(new ArrayList<boa.types.Ast.Statement>());
			l.visit(this);
			for(boa.types.Ast.Statement s : statements.pop())
				b.addStatements(s);
		}
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getStatement().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		eb.setLiteral(node.toSource());
		b.setExpression(eb.build());
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(FunctionDeclaration node) {
		List<boa.types.Ast.Method> list = methods.peek();
		Method.Builder b = Method.newBuilder();
		if (node.isConstructor())
			b.setName("<init>");
		else if (node.getName() != null)
			b.setName(node.getName().getFullyQualifiedName());
		for (Object m : node.modifiers()) {
			((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
			b.addModifiers(modifiers.pop());
		}
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		if (node.getReturnType2() != null) {
			String name = typeName(node.getReturnType2());
			for (int i = 0; i < node.getExtraDimensions(); i++)
				name += "[]";
			tb.setName(name);
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			b.setReturnType(tb.build());
		} else {
			tb.setName("void");
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			b.setReturnType(tb.build());
		}
		for (Object o : node.parameters()) {
			SingleVariableDeclaration ex = (SingleVariableDeclaration) o;
			Variable.Builder vb = Variable.newBuilder();
			vb.setName(ex.getName().getFullyQualifiedName());
			for (Object m : ex.modifiers()) {
				((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
				vb.addModifiers(modifiers.pop());
			}
			boa.types.Ast.Type.Builder tp = boa.types.Ast.Type.newBuilder();
			String name = typeName(ex.getType());
			for (int i = 0; i < ex.getExtraDimensions(); i++)
				name += "[]";
			if (ex.isVarargs())
				name += "...";
			tp.setName(name);
			tp.setKind(boa.types.Ast.TypeKind.OTHER);
			vb.setVariableType(tp.build());
			if (ex.getInitializer() != null) {
				ex.getInitializer().accept(this);
				vb.setInitializer(expressions.pop());
			}
			b.addArguments(vb.build());
		}
		for (Object o : node.thrownExceptions()) {
			boa.types.Ast.Type.Builder tp = boa.types.Ast.Type.newBuilder();
			tp.setName(((Name) o).getFullyQualifiedName());
			tp.setKind(boa.types.Ast.TypeKind.CLASS);
			b.addExceptionTypes(tp.build());
		}
		if (node.getBody() != null) {
			statements.push(new ArrayList<boa.types.Ast.Statement>());
			node.getBody().accept(this);
			for (boa.types.Ast.Statement s : statements.pop())
				b.addStatements(s);
		}
		list.add(b.build());
		return false;
	}

	public boolean visit(ReturnStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.RETURN);
		if (node.getReturnValue() != null) {
			node.getReturnValue().visit(this);
			b.setExpression(expressions.pop());
		}
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.EXPRESSION);
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.METHODCALL);
		eb.setMethod("super");
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			eb.addExpressions(expressions.pop());
		}
		for (Object a : node.arguments()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) a).accept(this);
			eb.addMethodArgs(expressions.pop());
		}
		for (Object t : node.typeArguments()) {
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			tb.setName(typeName((org.eclipse.wst.jsdt.core.dom.Type) t));
			tb.setKind(boa.types.Ast.TypeKind.GENERIC);
			eb.addGenericParameters(tb.build());
		}
		b.setExpression(eb.build());
		list.add(b.build());
		return false;
	}

	public boolean visit(SwitchCase node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.CASE);
		if (node.getExpression() != null) {
			node.getExpression().visit(this);
			b.setExpression(expressions.pop());
		}
		list.add(b.build());
		return false;
	}

	public boolean visit(SwitchStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.SWITCH);
		node.getExpression().visit(this);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		Node s = node.getFirstChild();
		while(s != null)
			((AstNode) s).visit(this);
			s = s.getNext();
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	public boolean visit(ThrowStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.THROW);
		node.getExpression().visit(this);
		b.setExpression(expressions.pop());
		list.add(b.build());
		return false;
	}

	public boolean visit(TryStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.TRY);
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getTryBlock().visit(this);
		for (Object c : node.getCatchClauses())
			((CatchClause) c).visit(this);
		if (node.getFinallyBlock() != null)
			node.getFinallyBlock().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(TypeDeclarationStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.TYPEDECL);
		declarations.push(new ArrayList<boa.types.Ast.Declaration>());
		node.getDeclaration().accept(this);
		for (boa.types.Ast.Declaration d : declarations.pop())
			b.setTypeDeclaration(d);
		list.add(b.build());
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationStatement node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.EXPRESSION);
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.VARDECL);
		for (Object o : node.fragments()) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) o;
			Variable.Builder vb = Variable.newBuilder();
			vb.setName(f.getName().getFullyQualifiedName());
			for (Object m : node.modifiers()) {
				((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
				vb.addModifiers(modifiers.pop());
			}
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			if (node.getType() != null) {
				String name = typeName(node.getType());
				for (int i = 0; i < f.getExtraDimensions(); i++)
					name += "[]";
				tb.setName(name);
			}
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			vb.setVariableType(tb.build());
			if (f.getInitializer() != null) {
				f.getInitializer().accept(this);
				vb.setInitializer(expressions.pop());
			}
			eb.addVariableDecls(vb.build());
		}
		b.setExpression(eb.build());
		list.add(b.build());
		return false;
	}

	public boolean visit(WhileLoop node) {
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.WHILE);
		node.getCondition().visit(this);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().visit(this);
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		if(node.getContinue() != null)
			((AstNode) node.getContinue()).visit(this);
		list.add(b.build());
		return false;
	}
	
	public boolean visist(Label node){
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.OTHER);
		if(node.getLoop() != null)
			node.getLoop().visit(this);
		if(node.getContinue() != null)
			((AstNode) node.getContinue()).visit(this);
		list.add(b.build());
		return false;
	}
	
	
	/*
	public boolean visit(Jump node){
		boa.types.Ast.Statement.Builder b = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		b.setKind(boa.types.Ast.Statement.StatementKind.JUMP);
		b.setExpression(expressions.pop());
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		for (boa.types.Ast.Statement s : statements.pop())
			b.addStatements(s);
		if(node.getJumpStatement() != null)
			node.getJumpStatement().visit(this);
		if(node.getLoop() != null)
			node.getLoop().visit(this);
		if(node.getContinue() != null)
			((AstNode) node.getContinue()).visit(this);
		list.add(b.build());
		return false;
	}*/

	//////////////////////////////////////////////////////////////
	// Expressions

	public boolean visit(ElementGet node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.ARRAYINDEX);
		node.getTarget().visit(this);
		b.addExpressions(expressions.pop());
		node.getElement().visit(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(ArrayLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		tb.setName(node.toSource());//typeName(node.getType()));
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		b.setNewType(tb.build());
		for (Object e : node.getElements()) {
			((AstNode) e).visit(this);
			b.addExpressions(expressions.pop());
		}
		expressions.push(b.build());
		return false;
	}

	public boolean visit(ArrayComprehension node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.NEWARRAY);
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		tb.setName(Token.typeToName(node.getType()));
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		b.setNewType(tb.build());
		for (Object l : node.getLoops()) {
			((AstNode) l).visit(this);
			b.addExpressions(expressions.pop());
		}
		for (Object s : node.getStatements()) {
			((AstNode) s).visit(this);
			b.addExpressions(expressions.pop());
		}
		
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(ArrayComprehensionLoop node) {
		boa.types.Ast.Statement.Builder s = boa.types.Ast.Statement.newBuilder();
		List<boa.types.Ast.Statement> list = statements.peek();
		s.setKind(boa.types.Ast.Statement.StatementKind.FOR);
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getBody().visit(this);
		for (boa.types.Ast.Statement x : statements.pop())
			s.addStatements(x);
		statements.push(new ArrayList<boa.types.Ast.Statement>());
		node.getIterator().visit(this);
		for (boa.types.Ast.Statement x : statements.pop())
			s.addStatements(x);
		node.getIteratedObject().visit(this);
		s.setExpression(expressions.pop());
		list.add(s.build());
		return false;
	}

	public boolean visit(VariableInitializer node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		node.getTarget().visit(this);
		b.addExpressions(expressions.pop());
		if(node.getInitializer() != null){
			node.getInitializer().visit(this);
			b.addExpressions(expressions.pop());
		}
		expressions.push(b.build());
		return false;
	}

	public boolean visit(Assignment node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		node.getLeft().visit(this);
		b.addExpressions(expressions.pop());
		node.getRight().visit(this);
		b.addExpressions(expressions.pop());
		if (node.getOperator() == Token.ASSIGN)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN);
		else if (node.getOperator() == Token.ASSIGN_BITAND)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_BITAND);
		else if (node.getOperator() == Token.ASSIGN_BITOR)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_BITOR);
		else if (node.getOperator() == Token.ASSIGN_BITXOR)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_BITXOR);
		else if (node.getOperator() == Token.ASSIGN_DIV)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_DIV);
		else if (node.getOperator() == Token.ASSIGN_LSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_LSHIFT);
		else if (node.getOperator() == Token.ASSIGN_SUB)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_SUB);
		else if (node.getOperator() == Token.ASSIGN_ADD)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_ADD);
		else if (node.getOperator() == Token.ASSIGN_MOD)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_MOD);
		else if (node.getOperator() == Token.ASSIGN_RSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_RSHIFT);
		else if (node.getOperator() == Token.ASSIGN_URSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_UNSIGNEDRSHIFT);
		else if (node.getOperator() == Token.ASSIGN_MUL)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.ASSIGN_MULT);
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(BooleanLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		if (node.booleanValue())
			b.setLiteral("true");
		else
			b.setLiteral("false");
		expressions.push(b.build());
		return false;
	}

	public boolean visit(KeywordLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral(Token.typeToName(node.getType()));
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.NEW);
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		tb.setName(typeName(node.getType()));
		tb.setKind(boa.types.Ast.TypeKind.CLASS);
		b.setNewType(tb.build());
		for (Object t : node.typeArguments()) {
			boa.types.Ast.Type.Builder gtb = boa.types.Ast.Type.newBuilder();
			gtb.setName(typeName((org.eclipse.wst.jsdt.core.dom.Type) t));
			gtb.setKind(boa.types.Ast.TypeKind.GENERIC);
			b.addGenericParameters(gtb.build());
		}
		if (node.getExpression() != null) {
			node.getExpression().accept(this);
			b.addExpressions(expressions.pop());
		}
		for (Object a : node.arguments()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) a).accept(this);
			b.addExpressions(expressions.pop());
		}
		if (node.getAnonymousClassDeclaration() != null) {
			declarations.push(new ArrayList<boa.types.Ast.Declaration>());
			node.getAnonymousClassDeclaration().accept(this);
			for (boa.types.Ast.Declaration d : declarations.pop())
				b.setAnonDeclaration(d);
		}
		expressions.push(b.build());
		return false;
	}

	public boolean visit(ConditionalExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.CONDITIONAL);
		node.getTestExpression().visit(this);
		b.addExpressions(expressions.pop());
		node.getTrueExpression().visit(this);
		b.addExpressions(expressions.pop());
		node.getFalseExpression().visit(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(FunctionExpression node) {
		boa.types.Ast.Expression.Builder bui = boa.types.Ast.Expression.newBuilder();
		bui.setKind(boa.types.Ast.Expression.ExpressionKind.CAST);
		methods.push(new ArrayList<boa.types.Ast.Method>());
		node.getMethod().accept(this);
		for (boa.types.Ast.Method d : methods.pop())
			b.addMethods(d);
		expressions.push(bui.build());
		return false;
	}

	@Override
	public boolean visit(FieldAccess node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.VARACCESS);
		node.getExpression().accept(this);
		b.addExpressions(expressions.pop());
		b.setVariable(node.getName().getFullyQualifiedName());
		expressions.push(b.build());
		return false;
	}

	
	public boolean visit(Name node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.VARACCESS);
		b.setVariable(node.getIdentifier());// getFullyQualifiedName());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(QualifiedName node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.VARACCESS);
		b.setVariable(node.getFullyQualifiedName());
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(PropertyGet node){
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		node.getProperty().visit(this);
		b.addExpressions(expressions.pop());
		node.getTarget();
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}

	public boolean visit(InfixExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		if (node.getOperator() == Token.BITAND)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_AND);
		else if (node.getOperator() == Token.AND)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LOGICAL_AND);
		else if (node.getOperator() == Token.OR)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LOGICAL_OR);
		else if (node.getOperator() == Token.DIV)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_DIV);
		else if (node.getOperator() == Token.EQ)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.EQ);
		else if (node.getOperator() == Token.GT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.GT);
		else if (node.getOperator() == Token.GE)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.GTEQ);
		else if (node.getOperator() == Token.LSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_LSHIFT);
		else if (node.getOperator() == Token.LT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LT);
		else if (node.getOperator() == Token.LE)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LTEQ);
		else if (node.getOperator() == Token.SUB)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_SUB);
		else if (node.getOperator() == Token.NE)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.NEQ);
		else if (node.getOperator() == Token.BITOR)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_OR);
		else if (node.getOperator() == Token.ADD)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_ADD);
		else if (node.getOperator() == Token.MOD)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_MOD);
		else if (node.getOperator() == Token.RSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_RSHIFT);
		else if (node.getOperator() == Token.URSH)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_UNSIGNEDRSHIFT);
		else if (node.getOperator() == Token.MUL)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_MULT);
		else if (node.getOperator() == Token.BITXOR)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_XOR);
		else if (node.getOperator() == Token.SHEQ)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		else if (node.getOperator() == Token.SHNE)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		else if (node.getOperator() == Token.IN)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		else
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		node.getLeft().visit(this);
		b.addExpressions(expressions.pop());
		node.getRight().visit(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(NewExpression node){
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.NEW);
		if (node.getInitializer() != null)
			node.getInitializer().visit(this);
		if(expressions.peek() != null)
			b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(InstanceofExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.TYPECOMPARE);
		node.getLeftOperand().accept(this);
		b.addExpressions(expressions.pop());
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		tb.setName(typeName(node.getRightOperand()));
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		b.setNewType(tb.build());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(NullLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral("null");
		expressions.push(b.build());
		return false;
	}

	public boolean visit(ObjectLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral("object");
		expressions.push(b.build());
		return false;
	}

	public boolean visit(ObjectProperty node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		if (node.getLeft() != null) {
			node.getLeft().visit(this);
			b.addExpressions(expressions.pop());
		}
		if (node.getRight() != null) {
			node.getRight().visit(this);
			b.addExpressions(expressions.pop());
		}
		expressions.push(b.build());
		return false;
	}

	public boolean visit(NumberLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral("" + node.getNumber());
		expressions.push(b.build());
		return false;
	}

	public boolean visit(ParenthesizedExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.PAREN);
		node.getExpression().visit(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return true;
	}

	/*
	@Override
	public boolean visit(PostfixExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		if (node.getOperator() == PostfixExpression.Operator.DECREMENT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_DEC);
		else if (node.getOperator() == PostfixExpression.Operator.INCREMENT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_INC);
		node.getOperand().accept(this);
		b.addExpressions(expressions.pop());
		b.setIsPostfix(true);
		expressions.push(b.build());
		return false;
	}
	*/
	
	public boolean visit(UnaryExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		if (node.isPostfix()) {
			b.setIsPostfix(true);
		}
		if (node.getOperator() == Token.DEC)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_DEC);
		else if (node.getOperator() == Token.INC)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_INC);
		else if (node.getOperator() == Token.ADD)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_ADD);
		else if (node.getOperator() == Token.SUB)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_SUB);
		else if (node.getOperator() == Token.BITNOT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_NOT);
		else if (node.getOperator() == Token.NOT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LOGICAL_NOT);
		node.getOperand().visit(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(RegExpLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		b.setLiteral(node.getValue());
		expressions.push(b.build());
		return false;
	}

	/*
	@Override
	public boolean visit(PrefixExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		if (node.getOperator() == PrefixExpression.Operator.DECREMENT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_DEC);
		else if (node.getOperator() == PrefixExpression.Operator.INCREMENT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_INC);
		else if (node.getOperator() == PrefixExpression.Operator.PLUS)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_ADD);
		else if (node.getOperator() == PrefixExpression.Operator.MINUS)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.OP_SUB);
		else if (node.getOperator() == PrefixExpression.Operator.COMPLEMENT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.BIT_NOT);
		else if (node.getOperator() == PrefixExpression.Operator.NOT)
			b.setKind(boa.types.Ast.Expression.ExpressionKind.LOGICAL_NOT);
		node.getOperand().accept(this);
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}
	*/

	public boolean visit(StringLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral(node.getValue());
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.VARACCESS);
		String name = "super." + node.getName().getFullyQualifiedName();
		if (node.getQualifier() != null)
			name = node.getQualifier().getFullyQualifiedName() + "." + name;
		b.setVariable(name);
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.METHODCALL);
		String name = "super." + node.getName().getFullyQualifiedName();
		if (node.getQualifier() != null)
			name = node.getQualifier().getFullyQualifiedName() + "." + name;
		b.setMethod(name);
		for (Object a : node.arguments()) {
			((org.eclipse.wst.jsdt.core.dom.Expression) a).accept(this);
			b.addMethodArgs(expressions.pop());
		}
		for (Object t : node.typeArguments()) {
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			tb.setName(typeName((org.eclipse.wst.jsdt.core.dom.Type) t));
			tb.setKind(boa.types.Ast.TypeKind.GENERIC);
			b.addGenericParameters(tb.build());
		}
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(ThisExpression node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		String name = "";
		if (node.getQualifier() != null)
			name += node.getQualifier().getFullyQualifiedName() + ".";
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral(name + "this");
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(Yield node){
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		if (node.getValue() != null){
			node.getValue().visit(this);
			eb.addExpressions(expressions.pop());
		}
		expressions.push(eb.build());
		return false;
	}


	@Override
	public boolean visit(TypeLiteral node) {
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.LITERAL);
		b.setLiteral(typeName(node.getType()) + ".class");
		expressions.push(b.build());
		return false;
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.VARDECL);
		for (Object o : node.fragments()) {
			VariableDeclarationFragment f = (VariableDeclarationFragment) o;
			Variable.Builder b = Variable.newBuilder();
			b.setName(f.getName().getFullyQualifiedName());
			for (Object m : node.modifiers()) {
				((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
				b.addModifiers(modifiers.pop());
			}
			boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
			String name = typeName(node.getType());
			for (int i = 0; i < f.getExtraDimensions(); i++)
				name += "[]";
			tb.setName(name);
			tb.setKind(boa.types.Ast.TypeKind.OTHER);
			b.setVariableType(tb.build());
			if (f.getInitializer() != null) {
				f.getInitializer().accept(this);
				b.setInitializer(expressions.pop());
			}
			eb.addVariableDecls(b.build());
		}
		expressions.push(eb.build());
		return false;
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		boa.types.Ast.Expression.Builder eb = boa.types.Ast.Expression.newBuilder();
		eb.setKind(boa.types.Ast.Expression.ExpressionKind.VARDECL);
		Variable.Builder b = Variable.newBuilder();
		b.setName(node.getName().getFullyQualifiedName());
		for (Object m : node.modifiers()) {
			((org.eclipse.wst.jsdt.core.dom.Modifier) m).accept(this);
			b.addModifiers(modifiers.pop());
		}
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		String name = typeName(node.getType());
		for (int i = 0; i < node.getExtraDimensions(); i++)
			name += "[]";
		tb.setName(name);
		tb.setKind(boa.types.Ast.TypeKind.OTHER);
		b.setVariableType(tb.build());
		if (node.getInitializer() != null) {
			node.getInitializer().accept(this);
			b.setInitializer(expressions.pop());
		}
		eb.addVariableDecls(b.build());
		expressions.push(eb.build());
		return false;
	}

	//////////////////////////////////////////////////////////////
	// Xml nodes
	
	
	
	public boolean visit(XmlDotQuery node){
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		node.getLeft().visit(this);
		b.addExpressions(expressions.pop());
		if (node.getRight() != null){
			node.getRight().visit(this);
			b.addExpressions(expressions.pop());
		}
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(XmlRef node){
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		
		boa.types.Ast.Type.Builder tb = boa.types.Ast.Type.newBuilder();
		String name = node.toSource();//Token.typeToName(node.getType());
		if (node.isAttributeAccess())
			name = "@" + name;
		tb.setName(name);
		b.setNewType(tb.build());
		expressions.push(b.build());
		return false;
	}
	
	public boolean visit(XmlExpression node){
		boa.types.Ast.Expression.Builder b = boa.types.Ast.Expression.newBuilder();
		b.setKind(boa.types.Ast.Expression.ExpressionKind.OTHER);
		node.getExpression();
		b.addExpressions(expressions.pop());
		expressions.push(b.build());
		return false;
	}



	//////////////////////////////////////////////////////////////
	// Currently un-used node types

	

	
	public boolean visit(Scope node) {
		throw new RuntimeException("visited unused node PrimitiveType");
	}

	
	public boolean visit(ScriptNode node) {
		throw new RuntimeException("visited unused node QualifiedType");
	}

	@Override
	public boolean visit(SimpleType node) {
		throw new RuntimeException("visited unused node SimpleType");
	}

	@Override
	public boolean visit(ImportDeclaration node) {
		throw new RuntimeException("visited unused node ImportDeclaration");
	}

	@Override
	public boolean visit(PackageDeclaration node) {
		throw new RuntimeException("visited unused node PackageDeclaration");
	}

	@Override
	public boolean visit(FunctionRef node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(FunctionRefParameter node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(MemberRef node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(TagElement node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(InferredType node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	@Override
	public boolean visit(UndefinedLiteral node) {
		throw new RuntimeException("visited unused node " + node.getClass().getSimpleName());
	}

	private int getIndex(String name) {
		Integer index = this.nameIndices.get(name);
		if (index == null) {
			index = this.nameIndices.size();
			this.nameIndices.put(name, index);
		}
		return index;
	}

	@Override
	public boolean visit(AstNode node) {
		// TODO Auto-generated method stub
		return false;
	}
}