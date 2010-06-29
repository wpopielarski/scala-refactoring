/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.refactoring
package tests.sourcegen

import tests.util.TestHelper
import org.junit.Assert._
import common.{SilentTracing, PimpedTrees}
import sourcegen.TreeChangesDiscoverer

class TreeChangesDiscovererTest extends TestHelper with PimpedTrees with TreeChangesDiscoverer with SilentTracing {
  
  override def treeForFile(file: AbstractFile) = {
    global.unitOfFile.get(file) map (_.body) flatMap removeAuxiliaryTrees
  }
  
  import global._
  
  val reverseBody = transform {
    case t: Template => t.copy(body = t.body.reverse) setPos t.pos
  }
  
  val doubleAllDefNames = transform {
    case t: DefDef => t.copy(name = t.name.toString + t.name.toString) setPos t.pos
  }
  
  val incrementIntegers = transform {
    case t @ Literal(c) if c.tag == IntTag => Literal(Constant(c.intValue + 1)) setPos t.pos
  }
  
  val wrapDefRhsInBlock = transform {
    case t @ DefDef(_, _, _, _, _, _: Block) => t
    case t @ DefDef(_, _, _, _, _, rhs) => t copy (rhs = new Block(Nil, rhs)) setPos t.pos
  }
    
  def transformAndFind(trans: Transformation[Tree, Tree], src: String) = {
    
    def describe(t: Tree) = if(t.pos == NoPosition) t.getClass.getSimpleName else t.getClass.getSimpleName +"("+ t.pos.line +")"

    val transformed = (removeAuxiliaryTrees &> trans)(treeFrom(src)).get

    findAllChangedTrees(transformed) map {
      case (t, c) => describe(t) +": "+ c.map(describe).mkString(", ")
    } mkString " | "
  }
  
  @Test
  def findChangedName1() {
    assertEquals("DefDef(4): DefDef(4), NameTree(4)", transformAndFind(↓(matchingChildren(doubleAllDefNames)), 
    """package findtest1

     class Test {
       def test = 42
       val test2 = 42
     }
    """))
  }
  
  @Test
  def findChangedName2() {
    assertEquals("DefDef(5): NameTree(6), Block(5), DefDef(6), DefDef(5), NameTree(5)", transformAndFind(↓(matchingChildren(doubleAllDefNames)), 
    """package findtest2

     object A {
       class Test {
         def test() = {
           def inner() = 10
         }
       }
     }
     """))
  }
  
  @Test
  def findChangedName3() {
    assertEquals("DefDef(4): DefDef(4), NameTree(4) | DefDef(5): DefDef(5), NameTree(5) | DefDef(6): DefDef(6), NameTree(6)", transformAndFind(↓(matchingChildren(doubleAllDefNames)), 
    """package findtest1

     class Test {
       def test1 = 42
       def test2 = 42
       def test3 = 42
     }
    """))
  }
  
  @Test
  def findReversedBody() {
    assertEquals("Template(3): Template(3)", transformAndFind(↓(matchingChildren(reverseBody)), 
    """package findtest3

     class Test {
       def test = 42
       val test2 = 42
     }
    """))
  }
  
  @Test
  def findReversedBodyAndIncrement() {
    assertEquals("Template(3): DefDef(4), Literal(5), Template(3), ValDef(5), Literal(4)", transformAndFind(↓(matchingChildren(reverseBody |> incrementIntegers)), 
    """package findtest3

     class Test {
       def test = 42
       val test2 = 42
     }
    """))
  }
  
  @Test
  def findIncrementedInts() {
    assertEquals("Literal(3): Literal(3) | Literal(4): Literal(4) | Literal(4): Literal(4) | Literal(4): Literal(4)", transformAndFind(↓(matchingChildren(incrementIntegers)), 
    """
     class Test {
       def test = 42
       List(1,2) map (_ + 1)
     }
    """))
  }
  
  @Test
  def findAddedBlock() {
    assertEquals("DefDef(3): DefDef(3), Block", transformAndFind(↓(matchingChildren(wrapDefRhsInBlock)), 
    """
     class Test {
       def test = 42
     }
    """))
  }
  
  @Test
  def findNestedChanges() {
    assertEquals("DefDef(3): Function(5), Apply(5), Block(3), DefDef(3), Template(4), Literal(5), ApplyToImplicitArgs(5), NameTree(3), ModuleDef(4)", 
      transformAndFind(↓(matchingChildren(doubleAllDefNames |> incrementIntegers)), 
    """
     class Test {
       def method(a: List[Int]) {
         object Inner {
           a map (_ + 1)
         }
       }
     }
    """))
  }
}





