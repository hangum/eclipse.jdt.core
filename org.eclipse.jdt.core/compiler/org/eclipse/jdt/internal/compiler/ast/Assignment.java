/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Genady Beriozkin - added support for reporting assignment with no effect
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 * 							bug 319201 - [null] no warning when unboxing SingleNameReference causes NPE
 * 							bug 292478 - Report potentially null across variable assignment
 *     						bug 335093 - [compiler][null] minimal hook for future null annotation support
 *     						bug 349326 - [1.7] new warning for missing try-with-resources
 *							bug 186342 - [compiler][null] Using annotations for null checking
 *							bug 358903 - Filter practically unimportant resource leak warnings
 *							bug 370639 - [compiler][resource] restore the default for resource leak warnings
 *							bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *							bug 345305 - [compiler][null] Compiler misidentifies a case of "variable can only be null"
 *							bug 388996 - [compiler][resource] Incorrect 'potential resource leak'
 *							bug 394768 - [compiler][resource] Incorrect resource leak warning when creating stream in conditional
 *							bug 395002 - Self bound generic class doesn't resolve bounds properly for wildcards for certain parametrisation.
 *							bug 331649 - [compiler][null] consider null annotations for fields
 *							bug 383368 - [compiler][null] syntactic null analysis for field references
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.codegen.*;
import org.eclipse.jdt.internal.compiler.flow.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.*;

public class Assignment extends Expression {

	public Expression lhs;
	public Expression expression;

public Assignment(Expression lhs, Expression expression, int sourceEnd) {
	//lhs is always a reference by construction ,
	//but is build as an expression ==> the checkcast cannot fail
	this.lhs = lhs;
	lhs.bits |= IsStrictlyAssigned; // tag lhs as assigned
	this.expression = expression;
	this.sourceStart = lhs.sourceStart;
	this.sourceEnd = sourceEnd;
}

public FlowInfo analyseCode(BlockScope currentScope, FlowContext flowContext, FlowInfo flowInfo) {
	// record setting a variable: various scenarii are possible, setting an array reference,
// a field reference, a blank final field reference, a field of an enclosing instance or
// just a local variable.
	LocalVariableBinding local = this.lhs.localVariableBinding();
	if ((this.expression.implicitConversion & TypeIds.UNBOXING) != 0) {
		this.expression.checkNPE(currentScope, flowContext, flowInfo);
	}
	
	FlowInfo preInitInfo = null;
	CompilerOptions compilerOptions = currentScope.compilerOptions();
	boolean shouldAnalyseResource = local != null
			&& flowInfo.reachMode() == FlowInfo.REACHABLE
			&& compilerOptions.analyseResourceLeaks
			&& (FakedTrackingVariable.isAnyCloseable(this.expression.resolvedType)
					|| this.expression.resolvedType == TypeBinding.NULL);
	if (shouldAnalyseResource) {
		preInitInfo = flowInfo.unconditionalCopy();
		// analysis of resource leaks needs additional context while analyzing the RHS:
		FakedTrackingVariable.preConnectTrackerAcrossAssignment(this, local, this.expression, flowInfo);
	}
	
	flowInfo = ((Reference) this.lhs)
		.analyseAssignment(currentScope, flowContext, flowInfo, this, false)
		.unconditionalInits();

	if (shouldAnalyseResource)
		FakedTrackingVariable.handleResourceAssignment(currentScope, preInitInfo, flowInfo, flowContext, this, this.expression, local);
	else
		FakedTrackingVariable.cleanUpAfterAssignment(currentScope, this.lhs.bits, this.expression);

	int nullStatus = this.expression.nullStatus(flowInfo, flowContext);
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		if (nullStatus == FlowInfo.NULL) {
			flowContext.recordUsingNullReference(currentScope, local, this.lhs,
				FlowContext.CAN_ONLY_NULL | FlowContext.IN_ASSIGNMENT, flowInfo);
		}
	}
	if (compilerOptions.isAnnotationBasedNullAnalysisEnabled) {
		VariableBinding var = this.lhs.nullAnnotatedVariableBinding();
		if (var != null) {
			nullStatus = checkAssignmentAgainstNullAnnotation(currentScope, flowContext, var, nullStatus, this.expression, this.expression.resolvedType);
			if (nullStatus == FlowInfo.NON_NULL
					&& var instanceof FieldBinding
					&& this.lhs instanceof Reference
					&& compilerOptions.enableSyntacticNullAnalysisForFields)
			{
				int timeToLive = (this.bits & InsideExpressionStatement) != 0
									? 2  // assignment is statement: make info survives the end of this statement
									: 1; // assignment is expression: expire on next event.
				flowContext.recordNullCheckedFieldReference((Reference) this.lhs, timeToLive);
			}
		}
	}
	if (local != null && (local.type.tagBits & TagBits.IsBaseType) == 0) {
		flowInfo.markNullStatus(local, nullStatus);
		if (flowContext.initsOnFinally != null)
			flowContext.markFinallyNullStatus(local, nullStatus);
	}
	return flowInfo;
}

void checkAssignment(BlockScope scope, TypeBinding lhsType, TypeBinding rhsType) {
	FieldBinding leftField = getLastField(this.lhs);
	if (leftField != null &&  rhsType != TypeBinding.NULL && (lhsType.kind() == Binding.WILDCARD_TYPE) && ((WildcardBinding)lhsType).boundKind != Wildcard.SUPER) {
	    scope.problemReporter().wildcardAssignment(lhsType, rhsType, this.expression);
	} else if (leftField != null && !leftField.isStatic() && leftField.declaringClass != null /*length pseudo field*/&& leftField.declaringClass.isRawType()) {
	    scope.problemReporter().unsafeRawFieldAssignment(leftField, rhsType, this.lhs);
	} else if (rhsType.needsUncheckedConversion(lhsType)) {
	    scope.problemReporter().unsafeTypeConversion(this.expression, rhsType, lhsType);
	}
}

public void generateCode(BlockScope currentScope, CodeStream codeStream, boolean valueRequired) {
	// various scenarii are possible, setting an array reference,
	// a field reference, a blank final field reference, a field of an enclosing instance or
	// just a local variable.

	int pc = codeStream.position;
	 ((Reference) this.lhs).generateAssignment(currentScope, codeStream, this, valueRequired);
	// variable may have been optimized out
	// the lhs is responsible to perform the implicitConversion generation for the assignment since optimized for unused local assignment.
	codeStream.recordPositionsFrom(pc, this.sourceStart);
}

FieldBinding getLastField(Expression someExpression) {
    if (someExpression instanceof SingleNameReference) {
        if ((someExpression.bits & RestrictiveFlagMASK) == Binding.FIELD) {
            return (FieldBinding) ((SingleNameReference)someExpression).binding;
        }
    } else if (someExpression instanceof FieldReference) {
        return ((FieldReference)someExpression).binding;
    } else if (someExpression instanceof QualifiedNameReference) {
        QualifiedNameReference qName = (QualifiedNameReference) someExpression;
        if (qName.otherBindings == null) {
        	if ((someExpression.bits & RestrictiveFlagMASK) == Binding.FIELD) {
        		return (FieldBinding)qName.binding;
        	}
        } else {
            return qName.otherBindings[qName.otherBindings.length - 1];
        }
    }
    return null;
}

public int nullStatus(FlowInfo flowInfo, FlowContext flowContext) {
	return this.expression.nullStatus(flowInfo, flowContext);
}

public StringBuffer print(int indent, StringBuffer output) {
	//no () when used as a statement
	printIndent(indent, output);
	return printExpressionNoParenthesis(indent, output);
}
public StringBuffer printExpression(int indent, StringBuffer output) {
	//subclass redefine printExpressionNoParenthesis()
	output.append('(');
	return printExpressionNoParenthesis(0, output).append(')');
}

public StringBuffer printExpressionNoParenthesis(int indent, StringBuffer output) {
	this.lhs.printExpression(indent, output).append(" = "); //$NON-NLS-1$
	return this.expression.printExpression(0, output);
}

public StringBuffer printStatement(int indent, StringBuffer output) {
	//no () when used as a statement
	return print(indent, output).append(';');
}

public TypeBinding resolveType(BlockScope scope) {
	// due to syntax lhs may be only a NameReference, a FieldReference or an ArrayReference
	this.constant = Constant.NotAConstant;
	if (!(this.lhs instanceof Reference) || this.lhs.isThis()) {
		scope.problemReporter().expressionShouldBeAVariable(this.lhs);
		return null;
	}
	TypeBinding lhsType = this.lhs.resolveType(scope);
	this.expression.setExpectedType(lhsType); // needed in case of generic method invocation
	if (lhsType != null) {
		this.resolvedType = lhsType.capture(scope, this.sourceEnd);
	}
	LocalVariableBinding localVariableBinding = this.lhs.localVariableBinding();
	if (localVariableBinding != null) {
		localVariableBinding.tagBits &= ~TagBits.IsEffectivelyFinal;
	}
	TypeBinding rhsType = this.expression.resolveType(scope);
	if (lhsType == null || rhsType == null) {
		return null;
	}
	// check for assignment with no effect
	Binding left = getDirectBinding(this.lhs);
	if (left != null && !left.isVolatile() && left == getDirectBinding(this.expression)) {
		scope.problemReporter().assignmentHasNoEffect(this, left.shortReadableName());
	}

	// Compile-time conversion of base-types : implicit narrowing integer into byte/short/character
	// may require to widen the rhs expression at runtime
	if (lhsType != rhsType) { // must call before computeConversion() and typeMismatchError()
		scope.compilationUnitScope().recordTypeConversion(lhsType, rhsType);
	}
	if (this.expression.isConstantValueOfTypeAssignableToType(rhsType, lhsType)
			|| rhsType.isCompatibleWith(lhsType, scope)) {
		this.expression.computeConversion(scope, lhsType, rhsType);
		checkAssignment(scope, lhsType, rhsType);
		if (this.expression instanceof CastExpression
				&& (this.expression.bits & ASTNode.UnnecessaryCast) == 0) {
			CastExpression.checkNeedForAssignedCast(scope, lhsType, (CastExpression) this.expression);
		}
		return this.resolvedType;
	} else if (isBoxingCompatible(rhsType, lhsType, this.expression, scope)) {
		this.expression.computeConversion(scope, lhsType, rhsType);
		if (this.expression instanceof CastExpression
				&& (this.expression.bits & ASTNode.UnnecessaryCast) == 0) {
			CastExpression.checkNeedForAssignedCast(scope, lhsType, (CastExpression) this.expression);
		}
		return this.resolvedType;
	}
	scope.problemReporter().typeMismatchError(rhsType, lhsType, this.expression, this.lhs);
	return lhsType;
}

/**
 * @see org.eclipse.jdt.internal.compiler.ast.Expression#resolveTypeExpecting(org.eclipse.jdt.internal.compiler.lookup.BlockScope, org.eclipse.jdt.internal.compiler.lookup.TypeBinding)
 */
public TypeBinding resolveTypeExpecting(BlockScope scope, TypeBinding expectedType) {

	TypeBinding type = super.resolveTypeExpecting(scope, expectedType);
	if (type == null) return null;
	TypeBinding lhsType = this.resolvedType;
	TypeBinding rhsType = this.expression.resolvedType;
	// signal possible accidental boolean assignment (instead of using '==' operator)
	if (expectedType == TypeBinding.BOOLEAN
			&& lhsType == TypeBinding.BOOLEAN
			&& (this.lhs.bits & IsStrictlyAssigned) != 0) {
		scope.problemReporter().possibleAccidentalBooleanAssignment(this);
	}
	checkAssignment(scope, lhsType, rhsType);
	return type;
}

public void traverse(ASTVisitor visitor, BlockScope scope) {
	if (visitor.visit(this, scope)) {
		this.lhs.traverse(visitor, scope);
		this.expression.traverse(visitor, scope);
	}
	visitor.endVisit(this, scope);
}
public LocalVariableBinding localVariableBinding() {
	return this.lhs.localVariableBinding();
}
}
