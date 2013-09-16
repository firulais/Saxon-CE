package client.net.sf.saxon.ce.expr.instruct;

import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.ExpressionTool;
import client.net.sf.saxon.ce.expr.XPathContext;
import client.net.sf.saxon.ce.om.Sequence;
import client.net.sf.saxon.ce.trans.XPathException;

import java.util.Iterator;

/**
 * The compiled form of an xsl:param element within a template in an XSLT stylesheet.
 *
 * <p>The xsl:param element in XSLT has mandatory attribute name and optional attribute select. It can also
 * be specified as required="yes" or required="no".</p>
 *
 * <p>This is used only for parameters to XSLT templates. For function calls, the caller of the function
 * places supplied arguments onto the callee's stackframe and the callee does not need to do anything.
 * Global parameters (XQuery external variables) are handled using {@link GlobalParam}.</p>
 *
 */

public final class LocalParam extends GeneralVariable {

    private int parameterId;
    private Expression conversion = null;
    private int conversionEvaluationMode = ExpressionTool.UNDECIDED;

    /**
     * Allocate a number which is essentially an alias for the parameter name,
     * unique within a stylesheet
     * @param id the parameter id
     */

    public void setParameterId(int id) {
        parameterId = id;
    }

    /**
     * Get the parameter id, which is essentially an alias for the parameter name,
     * unique within a stylesheet
     * @return the parameter id
     */

    public int getParameterId() {
        return parameterId;
    }
    
    /**
     * Define a conversion that is to be applied to the supplied parameter value.
     * @param convertor The expression to be applied. This performs type checking,
     * and the basic conversions implied by function calling rules, for example
     * numeric promotion, atomization, and conversion of untyped atomic values to
     * a required type. The conversion uses the actual parameter value as input,
     * referencing it using a VariableReference.
     */
    public void setConversion(Expression convertor) {
        conversion = convertor;
        if (convertor != null) {
            conversionEvaluationMode = ExpressionTool.eagerEvaluationMode(conversion);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator<Expression> iterateSubExpressions() {
        return nonNullChildren(select, conversion);
    }


    /**
    * Process the local parameter declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        int wasSupplied = context.useLocalParameter(getVariableQName(), this, isTunnelParam());
        switch (wasSupplied) {
        case ParameterSet.SUPPLIED_AND_CHECKED:
            // No action needed
            break;

        case ParameterSet.SUPPLIED:
            // if a parameter was supplied by the caller, with no type-checking by the caller,
            // then we may need to convert it to the type required
            if (conversion != null) {
                context.setLocalVariable(getSlotNumber(),
                        ExpressionTool.evaluate(conversion, conversionEvaluationMode, context));
                // We do an eager evaluation here for safety, because the result of the
                // type conversion overwrites the slot where the actual supplied parameter
                // is contained.
            }
            break;

            // don't evaluate the default if a value has been supplied or if it has already been
            // evaluated by virtue of a forwards reference

        case ParameterSet.NOT_SUPPLIED:
            if (isImplicitlyRequiredParam()) {
                String name = "$" + getVariableQName().getDisplayName();
                throw new XPathException("A value must be supplied for parameter "
                        + name + " because " +
                        "the default value is not a valid instance of the required type", "XTDE0610");
            } else if (isRequiredParam()) {
                String name = "$" + getVariableQName().getDisplayName();
                throw new XPathException("No value supplied for required parameter " + name, "XTDE0700");
            }
            context.setLocalVariable(getSlotNumber(), getSelectValue(context));
        }
        return null;
    }

    /**
     * Evaluate the variable
     */

    public Sequence evaluateVariable(XPathContext c) {
        return c.evaluateLocalVariable(slotNumber);
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
