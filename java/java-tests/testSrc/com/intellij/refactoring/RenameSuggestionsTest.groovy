/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.codeInsight.lookup.LookupEx
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightCodeInsightTestCase
/**
 * User: anna
 */
class RenameSuggestionsTest extends LightCodeInsightTestCase {
  public void "test by parameter name"() {
    def text = """\
     class Test {
         void foo(int foo) {}
         {
             int bar = 0;
             foo(b<caret>ar);
         }
     }
   }
   """

    doTestSuggestionAvailable(text, "foo")
  }

  public void "test by super parameter name"() {
    def text = """\
     class Test {
         void foo(int foo) {}
     }
     
     class TestImpl extends Test {
         void foo(int foo<caret>1) {}
     }
   }
   """

    doTestSuggestionAvailable(text, "foo")
  }

  @Override
  protected Sdk getProjectJDK() {
    return IdeaTestUtil.getMockJdk18()
  }

  public void "test by Optional.of initializer"() {
    def suggestions = getNameSuggestions("""
import java.util.*;
class Foo {{
  Foo typeValue = null;
  Optional<Foo> <caret>o = Optional.of(typeValue);
}}
""")
    assert suggestions == ["typeValue1", "value", "foo", "fooOptional", "optional", "o"]
  }

  public void "test by Optional.ofNullable initializer"() {
    def suggestions = getNameSuggestions("""
import java.util.*;
class Foo {{
  Foo typeValue = this;
  Optional<Foo> <caret>o = Optional.ofNullable(typeValue);
}}
""")
    assert suggestions == ["typeValue1", "value", "foo", "fooOptional", "optional", "o"]
  }

  public void "test by Optional.of initializer with constructor"() {
    def suggestions = getNameSuggestions("""
import java.util.*;
class Foo {{
  Optional<Foo> <caret>o = Optional.ofNullable(new Foo());
}}
""")
    assert suggestions == ["foo", "fooOptional", "optional", "o"]
  }
  
  public void "test by Optional.flatMap"() {
    def suggestions = getNameSuggestions("""
import java.util.*;
class Foo {{
  Optional<Car> <caret>o = Optional.of(new Person()).flatMap(Person::getCar);
}}
class Person {
    Optional<Car> getCar() {}
}
class Car {}
""")
    assert suggestions == ["car", "carOptional", "optional", "o"]
  }

  private doTestSuggestionAvailable(String text, String suggestion) {
    def suggestions = getNameSuggestions(text)
    assert suggestion in suggestions
    
  }
  
  private List<String> getNameSuggestions(String text) {
    configure text
    def oldPreselectSetting = myEditor.settings.preselectRename
    try {
      TemplateManagerImpl.setTemplateTesting(getProject(), getTestRootDisposable());
      final PsiElement element = TargetElementUtilBase.findTargetElement(myEditor, TargetElementUtilBase.getInstance().getAllAccepted())

      assertNotNull(element)

      VariableInplaceRenameHandler handler = new VariableInplaceRenameHandler()


      handler.doRename(element, editor, null);
      
      LookupEx lookup = LookupManager.getActiveLookup(editor)
      assertNotNull(lookup)
      return lookup.items.collect { it.lookupString }
    }
    finally {
      myEditor.settings.preselectRename = oldPreselectSetting

      TemplateState state = TemplateManagerImpl.getTemplateState(editor)

      assertNotNull(state)

      state.gotoEnd(false)
    }
  }

  private def configure(String text) {
    configureFromFileText("a.java", text)
  }
}