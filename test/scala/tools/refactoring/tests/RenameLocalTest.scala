package scala.tools.refactoring.tests

import scala.tools.refactoring.tests.util.TestRefactoring
import scala.tools.refactoring.util.Tracing
import scala.tools.refactoring.util.SilentTracing
import scala.tools.refactoring.RenameLocal
import scala.tools.refactoring.tests.util.TestHelper
import org.junit.Test
import org.junit.Assert._

class RenameLocalTest extends TestHelper with TestRefactoring {
    
  implicit def stringToRefactoring(src: String) = new TestRefactoringImpl(src) {
    val refactoring = new RenameLocal(global) with /*Silent*/Tracing
    def rename(name: String, e: String) = doIt(e, new refactoring.RefactoringParameters {
      val newName = name
    })
  }
    
  @Test
  def renameLocalValue = """
    class A {
      def double(s: String) = s + s
      def extractFrom {
        val s = "hallo"
/*(*/   s   /*)*/ .length   
        double(s + "a")
      }
    }
    """ rename("b",
    """
    class A {
      def double(s: String) = s + s
      def extractFrom {
        val b = "hallo"
/*(*/   b   /*)*/ .length   
        double(b + "a")
      }
    }
    """)
        
  @Test
  def renameParameter = """
    class A {
      def rename(  a/*(*//*)*/  : String) {
        println(a)
      }
    }
    """ rename("b",
    """
    class A {
      def rename(  b: String) {
        println(b)
      }
    }
    """)
    
  @Test
  def renameMultiAssignment = """
    class A {
      def print {
        val (/*(*/a/*)*/, b) = (5, 6)
        println(a + b)
      }
    }
    """ rename("c",
    """
    class A {
      def print {
        val (/*(*/c/*)*/, b) = (5, 6)
        println(c + b)
      }
    }
    """)
    
  @Test
  def renameBinding = """
    class A {
      def print {
        1 match { case /*(*/ i /*)*/ => i }
      }
    }
    """ rename("integer",
    """
    class A {
      def print {
        1 match { case /*(*/ integer /*)*/ => integer }
      }
    }
    """)
}