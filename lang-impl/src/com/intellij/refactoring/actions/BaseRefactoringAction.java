package com.intellij.refactoring.actions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseRefactoringAction extends AnAction {
  protected abstract boolean isAvailableInEditorOnly();

  protected abstract boolean isEnabledOnElements(PsiElement[] elements);

  protected boolean isAvailableOnElementInEditor(final PsiElement element, final Editor editor) {
    return true;
  }

  @Nullable
  protected abstract RefactoringActionHandler getHandler(DataContext dataContext);

  public final void actionPerformed(AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    final Project project = e.getData(PlatformDataKeys.PROJECT);
    if (project == null) return;
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    final Editor editor = e.getData(PlatformDataKeys.EDITOR);
    final PsiElement[] elements = getPsiElementArray(dataContext);
    RefactoringActionHandler handler = getHandler(dataContext);
    if (handler == null) return;
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      DaemonCodeAnalyzer.getInstance(project).autoImportReferenceAtCursor(editor, file);
      handler.invoke(project, editor, file, dataContext);
    }
    else {
      handler.invoke(project, elements, dataContext);
    }
  }

  protected boolean isEnabledOnDataContext(DataContext dataContext) {
    return false;
  }

  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setVisible(true);
    presentation.setEnabled(true);
    DataContext dataContext = e.getDataContext();
    Project project = e.getData(PlatformDataKeys.PROJECT);
    if (project == null) {
      disableAction(e);
      return;
    }

    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    PsiFile file = e.getData(LangDataKeys.PSI_FILE);
    if (file != null) {
      if (file instanceof PsiCompiledElement || !isAvailableForFile(file)) {
        disableAction(e);
        return;
      }
    }

    if (editor != null) {
      PsiElement element = e.getData(LangDataKeys.PSI_ELEMENT);
      if (element == null || !isAvailableForLanguage(element.getLanguage())) {
        if (file == null) {
          disableAction(e);
          return;
        }
        element = getElementAtCaret(editor, file);
      }
      final boolean isEnabled = element != null && !(element instanceof SyntheticElement) && isAvailableForLanguage(element.getLanguage()) &&
        isAvailableOnElementInEditor(element, editor);
      if (!isEnabled) {
        disableAction(e);
      }

    }
    else {
      if (isAvailableInEditorOnly()) {
        disableAction(e);
        return;
      }
      final PsiElement[] elements = getPsiElementArray(dataContext);
      final boolean isEnabled = isEnabledOnDataContext(dataContext) || elements.length != 0 && isEnabledOnElements(elements);
      if (!isEnabled) {
        disableAction(e);
      }
    }
  }

  public static PsiElement getElementAtCaret(final Editor editor, final PsiFile file) {
    final int offset = fixCaretOffset(editor);
    PsiElement element = file.findElementAt(offset);
    if (element == null && offset == file.getTextLength()) {
      element = file.findElementAt(offset - 1);
    }

    if (element instanceof PsiWhiteSpace) {
      element = file.findElementAt(element.getTextRange().getStartOffset() - 1);
    }
    return element;
  }

  private static int fixCaretOffset(final Editor editor) {
    final int caret = editor.getCaretModel().getOffset();
    if (editor.getSelectionModel().hasSelection() && !editor.getSelectionModel().hasBlockSelection()) {
      if (caret == editor.getSelectionModel().getSelectionEnd()) {
        return Math.max(editor.getSelectionModel().getSelectionStart(), editor.getSelectionModel().getSelectionEnd() - 1);
      }
    }

    return caret;
  }

  private static void disableAction(final AnActionEvent e) {
    e.getPresentation().setEnabled(false);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setVisible(false);
    }
  }

  protected boolean isAvailableForLanguage(Language language) {
    return language.equals(StdFileTypes.JAVA.getLanguage());
  }

  protected boolean isAvailableForFile(PsiFile file) {
    return true;
  }

  @NotNull
  public static PsiElement[] getPsiElementArray(DataContext dataContext) {
    PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext);
    if (psiElements == null || psiElements.length == 0) {
      PsiElement element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
      if (element != null) {
        psiElements = new PsiElement[]{element};
      }
    }

    if (psiElements == null) return PsiElement.EMPTY_ARRAY;

    List<PsiElement> filtered = null;
    for (PsiElement element : psiElements) {
      if (element instanceof SyntheticElement) {
        if (filtered == null) filtered = new ArrayList<PsiElement>(Arrays.asList(element));
        filtered.remove(element);
      }
    }
    return filtered == null ? psiElements : filtered.toArray(new PsiElement[filtered.size()]);
  }

}