package com.intellij.lang.javascript.inspections.actionscript.fixes;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.javascript.JSBundle;
import com.intellij.lang.javascript.highlighting.JSFixFactory;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.ecmal4.JSSuperExpression;
import com.intellij.lang.javascript.validation.JSAnnotatorProblemReporter;
import com.intellij.lang.javascript.validation.JSConstructorChecker;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.javascript.validation.JSAnnotatingVisitor.getPlaceForNamedElementProblem;

public class ActionScriptConstructorChecker extends JSConstructorChecker {
  public ActionScriptConstructorChecker(@NotNull JSAnnotatorProblemReporter problemReporter) {
    super(problemReporter);
  }

  public void checkMissedConstructor(@NotNull JSClass jsClass) {
    if (jsClass.isInterface()) return;
    final JSFunction nontrivialSuperClassConstructor = getNontrivialSuperClassConstructor(jsClass);

    if (nontrivialSuperClassConstructor == null) {
      return;
    }

    final PsiElement place = getPlaceForNamedElementProblem(jsClass);
    Annotation annotation = myProblemReporter.registerGenericError(place, JSBundle
      .message("javascript.validation.message.missed.super.constructor.call"));

    annotation.registerFix(JSFixFactory.getInstance().addConstructorAndSuperInvocationFix(jsClass, nontrivialSuperClassConstructor));
  }

  @Override
  protected void checkInstanceMemberAccesses(@NotNull JSFunction constructor, @NotNull JSClass clazz) {

  }

  @Override
  @Contract("null -> null")
  protected JSCallExpression findAnyBaseConstructorCall(@Nullable JSFunction jsFunction) {
    if (jsFunction == null) return null;
    final JSSourceElement[] body = jsFunction.getBody();
    return body.length > 0 ? findBaseConstructorCall(((JSBlockStatement)body[0])) : null;
  }

  private static JSCallExpression findBaseConstructorCall(final JSBlockStatement blockStatement) {
    for (JSStatement statement : blockStatement.getStatements()) {
      JSExpression expr;
      if (statement instanceof JSExpressionStatement &&
          (expr = ((JSExpressionStatement)statement).getExpression()) instanceof JSCallExpression &&
          ((JSCallExpression)expr).getMethodExpression() instanceof JSSuperExpression) {
        return (JSCallExpression)expr;
      }
      else if (statement instanceof JSIfStatement) {
        // awful compiler somehow allows to call super() only in one branch of 'if' statement. Example in Starling framework sources: class starling.display.Image
        final JSStatement then = ((JSIfStatement)statement).getThen();
        if (then instanceof JSBlockStatement) {
          JSCallExpression call = findBaseConstructorCall((JSBlockStatement)then);
          if (call != null) {
            return call;
          }
        }
      }
    }

    return null;
  }
}
