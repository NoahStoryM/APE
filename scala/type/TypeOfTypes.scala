/** Unified Type System -- Any, AnyRef, AnyVal
  *
  * {{{
  * scala> :load TypeOfTypes.scala
  * scala> :javap UnifiedTypeSystem
  * }}}
  *
  * [[http://ktoso.github.io/scala-types-of-types/#unified-type-system-any-anyref-anyval]]
  */
object UnifiedTypeSystem {
  import scala.collection.mutable.ArrayBuffer

  class Person

  val allThings = ArrayBuffer[Any]()
  val myInt = 42

  allThings += myInt
  allThings += new Person()
  // 35: invokevirtual #47                 // Method myInt:()I
  // 38: invokestatic  #53                 // Method scala/runtime/BoxesRunTime.boxToInteger:(I)Ljava/lang/Integer;
  // 41: invokevirtual #57                 // Method scala/collection/mutable/ArrayBuffer.$plus$eq:(Ljava/lang/Object;)Lscala/collection/mutable/ArrayBuffer;
  //
  // You'll notice that myInt is still carrying the value of a int
  // primitive (this is visible as `I' at the end of the `myInt:()I'
  // invokevirtual call). Then, right before adding it to the
  // ArrayBuffer (41), scalac inserted a call to
  // BoxesRunTime.boxToInteger:(I)Ljava/lang/Integer (a small hint for
  // not frequent bytecode readers, the method it calls is: public
  // Integer boxToInteger(i: int)) (38). This way, by having a smart
  // compiler and treating everything as an object in this common
  // hierarchy we’re able to get away from the "but primitives are
  // different" edge-cases, at least at the level of our Scala source
  // code.
}

/** The Bottom Types -- Nothing and Null
  *
  * [[http://ktoso.github.io/scala-types-of-types/#the-bottom-types-nothing-and-null]]
  */
object BottomTypes {
  // `thing' is type Int but how type inferencer can still work, and
  // infer sound types when working with "weird" situation like
  // throwing exceptions?
  val thing: Int =
    if (false)
      42
    else
      throw new Exception("Whoops")
  // The previous expression types checks! The inferencer was able to
  // infer that the `thing' value will only ever be of type `Int'.
  // This is because fo the Bottom Type property of `Nothing'. Type
  // inference always looks for the "common type" of both branches in
  // an if statement and `Nothing' extends everything (i.e.: every
  // types has `Nothing' as a subtype). Thus `Nothing' is a subtype of
  // `Int' and the common type of both branches is `Int'.

  // The same reasoning can be applied to the second Bottom Type in
  // Scala: `Null'
  val otherThing: String =
    if (false)
      "Yay!"
    else
      null
  // Just keep in mind the `Nothing' extends anything (i.e.: `Any')
  // whereas `Null' extends `AnyVal'. To ensure that, use `:type'
  // command (which allows to get the type of an expression) in the
  // REPL:
  //
  // > :type -v throw new Exception()
  // // Type signature
  // Nothing
  //
  // // Internal Type structure
  // TypeRef(TypeSymbol(final abstract class Nothing extends Any))
  //
  // > :type -v null
  // // Type signature
  // Null
  //
  // // Internal Type structure
  // TypeRef(TypeSymbol(final abstract class Null extends AnyRef))
}

/** Type Variance in Scala
  *
  * Variance can be explained as "type compatible-ness" between types:
  * {{{
  * Name            Description     Scala syntax
  * --------------+---------------+--------------
  * Invariant       C[T'] ≠ C[T]    C[T]
  * Covariant       C[T'] <: C[T]   C[+T]
  * Contravariant   C[T'] :> C[T]   C[-T]
  * }}}
  *
  * In general, immutable collections are covariant and mutaboe
  * collections are invariant.
  *
  * [[http://ktoso.github.io/scala-types-of-types/#type-variance-in-scala]]
  */
object TypeVariance {
  class Fruit
  case class Apple() extends Fruit
  case class Orange() extends Fruit

  // In the first example we use List[+A].
  val l1: List[Apple] = Apple() :: Nil
  val l2: List[Fruit] = Orange() :: l1
  val l3: List[AnyRef] = "" :: l2
  // Having immutable collections co-variant is safe. The same connot
  // be said about mutable collections. The classic example is
  // Array[T] wich is invariant. This a common problem that Java
  // solves using dynammic checking.
  // val a: Array[Any] = Array[Int](1,2,3) // won't type check
}

/** Refined Types (refinements)
  *
  * Subclassing without naming the subclass.
  *
  * [[http://ktoso.github.io/scala-types-of-types/#type-variance-in-scala]]
  */
object RefinedTypes {
  class Entity {
    def persistForReal() = {}
  }

  trait Persister {
    def doPersist(e: Entity) = {
      e.persistForReal()
    }
  }

  // Our redifined instance (and type):
  val redefinedMockPersister = new Persister {
    override def doPersist(e: Entity) = ()
  }
}

// Package Object
// http://ktoso.github.io/scala-types-of-types/#package-object
//
// Package Object provide a useful pattern for "importing a bunch of
// stuff together" as well as being one of the places to look for
// implicit values.
//
// It’s a custom to put package object’s in a file called
// package.scala into the package they’re the object for.
//
// src/main/scala/com/garden/apples/package.scala
//
// package com.garden
//
// package object apples extends RedApplest with GreenApples {
//   val redApples = List(red1, red2)
//   val greenApples = List(green1, green2)
// }
//
// trait RedApples {
//   val red1, red2 = "red"
// }
//
// trait GreenApples {
//   val green1, green2 = "green"
// }
//
// On the usage side, when you import "the package", you import any
// state that is defined in the package side.
//
// import com.garden.apples._
// redApples foreach println

/** Type Alias
  *
  * [[http://ktoso.github.io/scala-types-of-types/#type-alias]]
  */
object TypeAlias {
  // It's a trick we can use to make our code more readable.
  type User = String
  type Age = Int

  // Name our primitives with aliases to write a Map definition that
  // "makes sense!"
  val data: Map[User, Age] = Map.empty
}

/** Abstract Type Member
  *
  * [[http://ktoso.github.io/scala-types-of-types/#abstract-type-member]]
  */
object AbstractTypeMember {
  // For Java folks, following may seem very similar to the
  // `SimplestContainer<A>' syntax. But, it's a bit more powerful.
  trait SimplestContainer {
    type A // Stands for `type A >: Nothing <: Any'
    def value: A
  }

  // Because type is abstract, Now we can constraint our container on
  // types. For example we can have a container that can only store
  // `Number' (from java.lang).
  //
  // /!\ Here we use constraint `>:' to say "any A wich is reductible
  // to a Number". This makes sens because scala offers an implicit
  // library which convert any number like into a `Number'.
  trait OnlyNumbersContainer extends SimplestContainer {
    type A >: Number
  }
  val ints1 = new OnlyNumbersContainer {
    def value = 12
  }

  // Or we can add constraint later in the class hierarchy, for
  // instance by mixing in a trait that states "only Numbers".
  //
  // /!\ Here we use constraint `>:' to say "any A wich is reductible
  // to a Number". This makes sens because scala offers an implicit
  // library which convert any number like into a `Number'.
  trait OnlyNumbers {
    type A >: Number
  }
  val ints2 = new SimplestContainer with OnlyNumbers {
    def value = 12
  }
  // The following won't type check because there is no implicit rules
  // which transform an `String' into a `Number'. If we want that this
  // expression type checks, we have to introduce a new implicit rule.
  // val strs = new SimplestContainer with OnlyNumbers {
  //   def value = ""
  // }
}

/** F-Bounded Type
  *
  * It enables a sort of "self-referential" type constraint using to
  * solve how to define a polymorphic function that when it's called
  * at a subtype level, the polymorphic function *retains* the type of
  * the subtype.
  *
  * [[http://ktoso.github.io/scala-types-of-types/#self-recursive-type]]
  * [[http://logji.blogspot.se/2012/11/f-bounded-type-polymorphism-give-up-now.html]]
  */
object FBoundedType {
  /** F-Bounded for parameters
    *
    * Use F-Bounded Type to add constraint on a polymorphic function.
    * In this case the polymorphic function only accepts as parameter
    * subtypes of the concrete instance.
    *
    * [[http://ktoso.github.io/scala-types-of-types/#self-recursive-type]]
    */
  object Test1 {
    trait Fruit[T <: Fruit[T]] {
      final def compareTo(other: T): Boolean =
        // implem doesn't matter in our example but the method should
        // be final for the correctness.
        true
    }

    class Apple extends Fruit[Apple]
    class Orange extends Fruit[Orange]

    val apple = new Apple
    val orange = new Orange

    // apple compareTo orange // won't type check
                              // > found: Orange, required: Apple
  }

  /** F-Bounded for return type
    *
    * Use F-Bounded Type to add constraint on a polymorphic function.
    * In this case the polymorphic function, when passed a value of
    * some subtype will always return a value of the same subtype as
    * its parameter.
    *
    * [[http://logji.blogspot.se/2012/11/f-bounded-type-polymorphism-give-up-now.html]]
    */
  object Test2 {
    trait Account[T <: Account[T]] {
      // Always returns a value of the same subtype as its concrete
      // instance.
      def addFunds(amount: Double): T
    }

    class BrokerageAccount(total: Double) extends Account[BrokerageAccount] {
      override def addFunds(amount: Double) =
        new BrokerageAccount(total + amount)
    }

    class SavingsAccount(total: Double) extends Account[SavingsAccount] {
      override def addFunds(amount: Double) =
        new SavingsAccount(total + amount)
    }

    object Account {
      // Always returns a value of the same subtype as its parameter.
      // When you pass a `SavingsAccount', the methods returns
      // something with the type `SavingAccount'. It works with one
      // account at a time.
      def deposit[T <: Account[T]](amount: Double, account: T): T =
        account.addFunds(amount)

      // Pending Questions:
      //
      // 1. How to properly refer to the abstract supertype?
      def debpositAll(amount: Double,
                      // We *existentially bounded the type of each
                      // member* of the List with the `forSome'
                      // keyword instead of *existentially bounded the
                      // type of List* as a whole.
                      accounts: List[T forSome { type T <: Account[T] }]):
                      List[T forSome { type T <: Account[T] }] =
        accounts map { deposit(amount, _) }
    }

    // 2. Am I sure that the type bound is "self-referential"? Nop!
    // The following type checks because F-Bounded parameter states
    // that a subtype must be parameterized by *some potentially
    // other subtype*.
    class MalignantAccount(total: Double) extends Account[SavingsAccount] {
      override def addFunds(amount: Double) =
        new SavingsAccount(total + amount)
    }
    // Fortunately, calling deposit on it won't type checks:
    // Account.deposit(100, new MalignantAccount(100))
  }
}

/** 13/14. Type Constructor
  *
  * Act like a function but on the type level.
  *
  * [[http://adriaanm.github.io/research/2010/10/06/new-in-scala-2.8-type-constructor-inference/]]
  */
// TODO


/** Self type
  *
  * A self type of a trait is the assumed type of `this', the
  * receiver, to be used within a trait. Any concrete class that mixes
  * in the trait must ensure that its type conforms to the trait's
  * self type.
  *
  * The most common use of self types if for dependency injection
  * [[http://youtu.be/hBVJbzAagfs]]
  *
  * See section 29.14 and 33.5 of Odersky's book "Progamming in
  * Scala".
  */
object SelfType {
  // Self type is used for Dependency Injection.
  trait A { def a = "A" }
  trait B { self: A => def b = a}
  //
  // new B {} // Doesn't compile
  // <console>:10: error: illegal inheritance;
  //  self-type B does not conform to B's selftype B with A
  //               new B {}
  //
  // To compile we have to feed the B with an A:
  new B with A {}
  // And this is better than `trait B extends A { def b = a }' because
  // this enable dependency injection. For instance we could define a
  // nex trait `BetterA' which is a subtype of A and the instanciate a
  // `B' with `BetterA' just like what we do with dependency
  // injection.
  trait BetterA extends A { override def a = "BetterA" }
  new B with BetterA {} // Feed `B' with `BetterA' rather than with a
                        // `A'.

  // [[http://stackoverflow.com/q/1990948]]
  // One can use type parameters within self types. Thus means that
  // `A' require `Self'.
  trait D[Self] { self: Self => }
  // Rather `trait D[Self] extends Self' isn't legal.

  // Self types also allow you to define cyclical dependencies.
  trait E { self: F => }
  trait F { self: E => }
  // Inheritance using extends does not allow that. Try:
  //
  // trait E extends F
  // trait F extends E
  //  error:  illegal cyclic reference involving trait E
  //
  // In the Odersky book, look at section 33.5 (Creating spreadsheet
  // UI chapter) where it mentions: In the spreadsheet example, class
  // Model inherits from Evaluator and thus gains access to its
  // evaluation method. To go the other way, class Evaluator defines
  // its self type to be Model, like this:
  //
  // package org.stairwaybook.scells
  // trait Evaluator { this: Model => ...
}

/** Type Class
  *
  * [[http://danielwestheide.com/blog/2013/02/06/the-neophytes-guide-to-scala-part-12-type-classes.html]]
  */
object TypeClass {
  /** Fancy statistics library: No polymorphisme */
  object StatisticsNoPolymorphisme {
    def median(xs: Vector[Double]): Double = xs(xs.size / 2)
    def quartiles(xs: Vector[Double]): (Double, Double, Double) =
      (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))
    def iqr(xs: Vector[Double]): Double = quartiles(xs) match {
      case (lowerQuartile, _, upperQuartile) => upperQuartile - lowerQuartile
    }
    def mean(xs: Vector[Double]): Double = xs.reduce(_ + _) / xs.size
  }

  // Now we want to support more than just double numbers. So let's
  // implement all these methods again for Int numbbers, right? No,
  // the type parameter suffers from type erasure!
  //
  //   def median(xs: Vector[Int]): Int = xs(xs.size / 2)
  //   def quartiles(xs: Vector[Int]): (Int, Int, Int) =
  //     (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))
  //
  // > [error] method quartiles:(xs: Vector[Int])(Int, Int, Int) and
  // > [error] method quartiles:(xs: Vector[Double])(Double, Double,
  // > Double) at line 9
  // > [error] have same type after erasure: (xs: collection.immutab
  // > le.Vector)Tuple3

  // As a solution, we could use subtype polymorphisme. If scala
  // offers a subtype `Number' such that Number :> Double and Number
  // :> Int, the library written to take a Number will work equally
  // well when passed an Int or Double as when passed a Number.
  //
  // /** Fancy statistics library: Subtype polymorphisme */
  // object StatisticsPolymorphisme {
  //   def median(xs: Vector[Number]): Number = xs(xs.size / 2)
  //   def quartiles(xs: Vector[Number]): (Number, Number, Number) =
  //     (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))
  //   def iqr(xs: Vector[Number]): Number = quartiles(xs) match {
  //     case (lowerQuartile, _, upperQuartile) =>
  //       upperQuartile - lowerQuartile
  //   }
  //   def mean(xs: Vector[Number]): Number = {
  //     xs.reduce(_ + _) / xs.size
  //   }
  // }
  //
  // But there is no such common trait in scala.
  //
  // Java developer solution is Adapter pattern.

  /** Fancy statistics library: Adapter pattern */
  object StatisticsAdapterPattern {
    trait NumberLike[A] {
      def get: A
      def plus(y: NumberLike[A]): NumberLike[A]
      def minus(y: NumberLike[A]): NumberLike[A]
      def divide(y: Int): NumberLike[A]
    }

    case class NumberLikeDouble(x: Double) extends NumberLike[Double] {
      def get: Double = x
      def minus(y: NumberLike[Double]) = NumberLikeDouble(x - y.get)
      def plus(y: NumberLike[Double]) = NumberLikeDouble(x + y.get)
      def divide(y: Int) = NumberLikeDouble(x / y)
    }

    case class NumberLikeInt(x: Int) extends NumberLike[Int] {
      def get: Int = x
      def minus(y: NumberLike[Int]) = NumberLikeInt(x - y.get)
      def plus(y: NumberLike[Int]) = NumberLikeInt(x + y.get)
      def divide(y: Int) = NumberLikeInt(x / y)
    }

    def median[A](xs: Vector[NumberLike[A]]): NumberLike[A] = xs(xs.size / 2)
    def quartiles[A](xs: Vector[NumberLike[A]]):
        (NumberLike[A], NumberLike[A], NumberLike[A]) =
      (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))
    def iqr[A](xs: Vector[NumberLike[A]]): NumberLike[A] =
      quartiles(xs) match {
        case (lowerQuartile, _, upperQuartile) =>
          upperQuartile.minus(lowerQuartile)
      }
    def mean[A](xs: Vector[NumberLike[A]]): NumberLike[A] =
      xs.reduce(_.plus(_)).divide(xs.size)
  }

  // Adapter is OK but two problems left:
  // - User of the library has to create instances of the adapter to
  //   interact with the library.
  // - User of the library has to pass a `NumberLike' which is tiresome
  //   to wrtie and read.

  object Math {
    // Interface with common operations on Number.
    trait NumberLike[T] {
      def plus(x: T, y: T): T
      def divide(x: T, y: Int): T
      def minus(x: T, y: T): T
    }
    object NumberLike {
      // Rule to apply operation on Double
      implicit object NumberLikeDouble extends NumberLike[Double] {
        def plus(x: Double, y: Double): Double = x + y
        def divide(x: Double, y: Int): Double = x / y
        def minus(x: Double, y: Double): Double = x - y
      }
      // Rule to apply operation on Int
      implicit object NumberLikeInt extends NumberLike[Int] {
        def plus(x: Int, y: Int): Int = x + y
        def divide(x: Int, y: Int): Int = x / y
        def minus(x: Int, y: Int): Int = x - y
      }
    }
  }

  object StatisticsTypeClass {
    // We put into StatisticsTypeClass's scope implicite rules of
    // Math.NumberLike
    import Math.NumberLike

    // The implicit indicates that if no `NumberLike[T]' is passes. Then
    // scala looks into the global scope.
    def mean[T](xs: Vector[T])(implicit ev: NumberLike[T]): T =
      ev.divide(xs.reduce(ev.plus(_, _)), xs.size)

    def median[T](xs: Vector[T]): T = xs(xs.size / 2)

    def quartiles[T](xs: Vector[T]): (T, T, T) =
      (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))

    def iqr[T](xs: Vector[T])(implicit ev: NumberLike[T]): T =
      quartiles(xs) match {
        case (lowerQuartile, _, upperQuartile) =>
          ev.minus(upperQuartile, lowerQuartile)
      }

    def main(args: Array[String]) {
      val strs = Vector("1", "2", "3", "4")
      println(StatisticsTypeClass.quartiles(strs))
    }
  }
}

/** Structural Type
  *
  * In general, with types we think in terms of "does it implement
  * interface X?". With structural types we can reason about the
  * structure of a given object. When checking whether a type matches
  * using structual typing, we need to change our question to:"does it
  * have a method with this signature?".
  */
object StructuralType {
  // French saying: "Il ne faut pas mélanger les Torchons et les
  // Serviettes" So in OOP there is no common supertype between
  // `Torchon' and `Serviette'.
  class Torchon { def wipe: Unit = ??? }
  class Serviette { def wipe: Unit = ??? }
  // But both of them have a wipe methode and we should use one or the
  // other to clean the `Table'. Because there is no common supertype
  // between `Torchon' and `Serviette', let's use Structural Type to
  // define the method clean.
  object Table {
    def clean(wiper: { def wipe: Unit }) =
      wiper.wipe
  }
  Table.clean(new Torchon())
  Table.clean(new Serviette())
  // Another fact to keep in mind when using Structural Typing is that
  // it actually *has huge (negative) runtime performance*
  // implications, as it is actually implemented using reflection. Use
  // javap to see it:
  /** {{{
    * Scala> :javap StructuralType$Table$
    * public void clean(java.lang.Object);
    *   ...
    * 3: invokevirtual #75                 // Method java/lang/Object.getClass:()Ljava/lang/Class;
    * 6: invokestatic  #77                 // Method reflMethod$Method1:(Ljava/lang/Class;)Ljava/lang/reflect/Method;
    * }}}
    */
}

/** Path Dependent Type
  *
  * A type of a inner class is dependent on the instance of the outer
  * class.
  *
  * [[http://ktoso.github.io/scala-types-of-types/#path-dependent-type]]
  */
object PathDependentType {
  class Outer { class Inner }

  val out1 = new Outer
  // scala> :type PathDependentType.out1
  // PathDependentType.Outer
  val out2 = new Outer
  // scala> :type PathDependentType.out2
  // PathDependentType.Outer
  val out1in = new out1.Inner
  // scala> :type PathDependentType.out1in
  // PathDependentType.out1.Inner
  val out2in = new out2.Inner
  // scala> :type PathDependentType.out2in
  // PathDependentType.out2.Inner

  class Parent { class Child }
  class ChildrenNursery(val p: Parent) {
    // Using the path dependent type we now encode in the type system,
    // the logic, that the container should only contain children of
    // this parent - and not "any parent". Notice the `val' in the
    // constructor which mades the atrribute `p' public. This is
    // required when we fixe the abstract type `ChildOfThisParent'.
    type ChildOfThisParent = p.Child
    def punish(c: ChildOfThisParent) = ???

    // If we want to refer to child of any parent we should use `#'
    // instead of `.'
    type ChildOfAnyParent = Parent#Child
    def cuddle(c: ChildOfAnyParent) = ???
  }
  val p1 = new Parent
  val nursery = new ChildrenNursery(p1)
  val p1child = new nursery.p.Child
  val p2 = new Parent
  val p2child = new p2.Child

  nursery.punish(p1child)
  // nursery.punish(p2child) // won't type checks
  nursery.cuddle(p1child)
  nursery.cuddle(p2child)
}

// Definition Universal vs Existential type
//
// TL;DR Si le type paramtétrique est important pour ta fonction,
// alors tu paramètres avec le type `T' => C'est de la quantification
// universelle. Si tu sais que tu travails sur une structure
// parmamétrée mais que tu te fiches de son type, alors tu paramètres
// avec le `_' => C'est la quantification existentielle. Over those
// two, you could add some constraint, i.e.: `>: <: =:='
//
// - Universal type: You can plug in whatever type *you want*, I don't
//   need to know anything about the type to do my job, I'll only
//   refer to it opaquely as `X'.
// - Existential type: I'll use whatever type *I want* here; you wont
//   know anything about the type, so you can only refer to it
//   opaquely as `X'
//
//  In Scala, `List[+A]' is universally quantified. A list takes
//  anything and this is not a problem because it only does storage.
//  In contrast, the method `to[Col[_]]: Col[A]' in
//  `DoubleLinkedList[+A]' is existentially quantified. This method
//  converts the `DoubleLinkedList[A]' to a collection `Col[A]'. To
//  define the form of the collection (e.g.: `Vector', `List', `Set',
//  ...) the method `to' takes in parameter the collection where the
//  type of the collection doesn't matter `Col[_]' because this the
//  method which is in charge of giving a type: I'll use whatever
//  type *I want* here;.
//
//  scala> import scala.collection.mutable.DoubleLinkedList
//  import scala.collection.mutable.DoubleLinkedList
//
//  scala> DoubleLinkedList(1,2)
//  res1: DoubleLinkedList[Int] = DoubleLinkedList(1, 2)
//
//  scala> res1.to[Vector]
//  res3: Vector[Int] = Vector(1, 2)
//
//  scala> res1.to[List]
//  res4: List[Int] = List(1, 2)
//
//  scala> res1.to[Set]
//  res2: Set[Int] = Set(1, 2)

/** Existential Type
  *
  * Scala use the forSome keyword to define existential types.
  * Existential Types are especially useful when we want to take in
  * parameter Non-Variant-Parametric-Class without fixing the type
  * parameter. See Map example.
  *
  * [[http://www.drmaciver.com/2008/03/existential-types-in-scala/]]
  */
object ExistentialType {
  import scala.collection.mutable.Map
  // Abstract Types `AbsType1' and `AbsType2' are equivalent. Every
  // expr considered as an `AbsType1' or `AbsType2' should ba
  // treated as an `Any'
  type AbsType1 = T forSome { type T }
  type AbsType2 = T forSome { type T <: Any }
  type AbsType3 = T forSome { type T <: AnyRef }

  // `forSome' keyword enables the definition of existential types.
  // See the difference between `f' and `g'. They look almost
  // identical but they're not. In `f', `T' is the type of all List,
  // whatever their type parameter. `g' is `List[Any]'.
  def f(l: List[T] forSome { type T }) = ???
  def g(l: List[T forSome { type T }]) = ???

  // def i(l: List[T <: AnyRef]) = ??? // Not a valid expr
  def i(l: List[T forSome { type T <: AnyRef }]) = ???
  // def j: T <: AnyRef = ??? // Not a valid expr
  def j: T forSome { type T <: AnyRef} = ???

  // Example, we want a map that maps classes to a
  // string.`java.lang.Class<T>' is a parametric class with `T' the
  // type of the class modeled by this Class object.
  //
  // Here are some propositions:
  val map1: Map[Class[T forSome { type T }], String] = Map.empty
  val map2: Map[Class[T] forSome { type T }, String] = Map.empty
  val map3: Map[Class[T], String] forSome { type T } = Map.empty
  //
  // And some tests:
  //
  // _map1_:
  // Type of map1 is equivalent to Map[Class[Any], String] and
  // Class[T] is invariant in its type parameter `T'. Thus, the
  // mapper only maps classOf[Any]
  map1.put(classOf[Any], "Any")
  // The following won't type checks because `Class[T]' is invariant
  // on `T'. This could be a good solution if `Class[T]' wasn't
  // invariant, because here we don't care of T we only want to
  // store it (just like what we do with existential quantification)
  // *but it doesn't type checks*.
  // mapper.map1.put(classOf[Int], "Int") // won't type checks
  //
  // _map3_:
  // It's the opposit of map1. map3 is the supertype of all map types
  // such that there is some T such that they are a Map[Class[T],
  // String]. So again, we've got some fixed class type for keys in
  // the map - it's just that this time we don't know what type it is.
  // _MON-INTUITION_: map3 fixe le `T' par `T' or le `T' n'est pas
  // connu. Par exemple si on pouvait fixer le `T' à `Int', on
  // pourrait y metrre que des `Int'.
  // mapper.map3.put(classOf[Int], "Int")} // Required Class[T]
  //
  // _map2_:
  // map2 has keys of type Class[T] forSome { type T }. That is, its
  // keys are classes which are allowed to have any value they want
  // for their type parameter. So this is what we actually wanted.
  map2.put(classOf[Any], "Any")
  map2.put(classOf[Int], "Int")
  map2.put(classOf[String], "String")
  // map2 could also be defined as Map[Class[_], String] that is the
  // existential quatifiaction.
}

/** View Bound
  *
  * Use some type `A' *as if it were* some type `B'. In other words,
  * `A' should have an implicit conversion to `B' available, so that
  * one can call `B' methods on an object of type `A'. The most common
  * usage of view bounds in the standard library is with
  * [[scala.math.Ordered[A]]].
  *
  * Notice that view bounds are deprecated
  * [[https://github.com/scala/scala/pull/2909]]
  *
  * [[http://stackoverflow.com/a/4467012]]
  */
object ViewBound {
  // Because one can convert `A' into an `Ordered[A]', and because
  // `Ordered[A]' defines the method `<(other: A): Boolean', I can use
  // the expression `a < b'.
  def f[A <% Ordered[A]](a: A, b: A): A = if (a < b) a else b

  // View bounds are mostly used in the "pimp my library pattern" that
  // decorates classes with additional methods and properties, in
  // situation where you want to return the original type somehow. The
  // previous example needs a view bound because it returns the
  // non-converted type.
  //
  // However, if I were to return another type, then I don't need a
  // view bound anymore:
  def g[A](a: Ordered[A], b: A): Boolean = a < b

  // In the scala implementation, view bound is syntatic sugare for
  // some implicit conversion function.
  def fUnsugared[A](a: A, b: A)(implicit ev: A => Ordered[A]) =
    if (a < b) a else b

  // ViewBound are deprecated. In the next version of Scala only
  // context bound subsists. Here is the tricks to make view bound
  // from context bound.
  type Ord[A] = A => Ordered[A]

  def ff[A : Ord](a: A, b: A): A = if (a < b) a else b
  def gg[A](a: Ordered[A], b: A): Boolean = a < b
  def ffUnsugared[A](a: A, b: A)(implicit ev: Ord[A]) =
                        /* i.e: (implicit ev: A => Ordered[A]) */
    if (a < b) a else b
}

/** Context Bound
  *
  * Declares that for some type `A', *there is an implicit value of*
  * type `B[A]'.Context bound are typically used with the "type class
  * pattern" (see [[TypeClass]]). While a view bound can be used with
  * a simple types (e.g.: `A <% String', a context bound required a
  * *parameterized type*, such as [[scala.math.Ordering[A]]]. A
  * context bound describes an implicit value, instead of view bound's
  * implicit conversion.
  *
  * [[http://stackoverflow.com/a/4467012]]
  */
object ContextBound {
  // We use `implicitely' to retrieve the implicit value we want, one
  // of type `Ordering[A]', which class defines the methode
  // `compare(a: A, b: A): Int'
  def g[A](value: A, ord: Ordering[A]) = ???
  def f[A : Ordering](a: A) = g(a, implicitly[Ordering[A]])

  // In the scala implementation, countext bound is syntatic sugare for
  // some implicit value.
  def fUnsugared[A](a: A)(implicit ev: Ordering[A]) = g(a, ev)

  // Context bounds are mainly used in what has becom know as
  // *typeclass pattern* (see [[TypeClass]]). In the following, we
  // rewrite the [[TypeClass]] example using context bound instead of
  // implicite value.
  import TypeClass.Math
  object StatisticsTypeClass {
    // We put into StatisticsTypeClass's scope implicite rules of
    // Math.NumberLike
    import Math.NumberLike

    // We use Context Bound instead of implicit. To get the
    // `NumberLike[T]' value, we use the key word
    // `implicitly[NumberLike[T]]'.
    def mean[T : NumberLike](xs: Vector[T]): T = {
      val ev = implicitly[NumberLike[T]]
      ev.divide(xs.reduce(ev.plus(_, _)), xs.size)
    }

    def median[T](xs: Vector[T]): T = xs(xs.size / 2)

    def quartiles[T](xs: Vector[T]): (T, T, T) =
      (xs(xs.size / 4), median(xs), xs(xs.size / 4 * 3))

    def iqr[T : NumberLike](xs: Vector[T]): T =
      quartiles(xs) match {
        case (lowerQuartile, _, upperQuartile) =>
          implicitly[NumberLike[T]].minus(upperQuartile,
            lowerQuartile)
      }
  }
  // A more complex example is the collection usage of `CanBuildFrom'.
}

/** PimpMyLibrary Pattern
  *
  * Pimp My Library pattern allows you to decorate classes with
  * additional methods and properties. The following is how you would
  * add a method "bling" to [[java.lang.String]] which will add
  * asterisks to either end.
  *
  * [[https://coderwall.com/p/k_1jzw/scala-s-pimp-my-library-pattern-example]]
  */
object PimpMyLibrary {
  implicit class BlingString(string: String) {
    def bling = "*" + string + "*"
  }

  implicit def blingYoString(string: String) =
    new BlingString(string)

  // scala> "Let's get blinged out!".bling
  // res0: java.lang.String = *Let's get blinged out!*

  // There is also a recomanded syntax based on AnyVal
  // [[http://docs.scala-lang.org/overviews/core/value-classes.html]]
  implicit class BlingStringRecommanded(val string: String) extends AnyVal {
    def bling = "*" + string + "*"
  }
}

/** Type-Level Computations
  *
  * Motivation:
  * - Unlike Scala's tuples: no size limit & can abstrat over arity
  * - No runtime runtime overhead & no implicits
  *
  * [[http://slick.typesafe.com/talks/scalaio2014/Type-Level_Computations.pdf]]
  */
object TypeLevelComputation {
  // ------------------------------ A simple boolean type (à la Smalltalk)
  // How you do this at Term-level (General case)
  object BooleanTermLevel {
    sealed trait Bool {
      def &&(b: Bool): Bool
      def ||(b: Bool): Bool
      def ifElse[B](t: => B, f: => B): B
    }

    lazy val True: Bool = new Bool {
      def &&(b: Bool): Bool = b
      def ||(b: Bool): Bool = True
      def ifElse[B](t: => B, f: => B): B = t
    }

    lazy val False: Bool = new Bool {
      def &&(b: Bool): Bool = False
      def ||(b: Bool): Bool = b
      def ifElse[B](t: => B, f: => B): B = f
    }

    object Test {
      assert ( (False  &&  False) == False )
      assert ( (False  &&  True)  == False )
      assert ( (True   &&  False) == False )
      assert ( (True   &&  True)  == True  )

      assert ( (False  ||  False) == False )
      assert ( (False  ||  True)  == True  )
      assert ( (True   ||  False) == True  )
      assert ( (True   ||  True)  == True  )

      assert ( False.ifElse (1,2) == 2     )
      assert ( True.ifElse  (1,2) == 1     )
    }
  }

  // Moves from Term Level to Type Level
  object BooleanTypeLevel {
    sealed trait Bool {
      // Translating Methods
      type &&[B <: Bool] <: Bool
      type ||[B <: Bool] <: Bool
      type IfElse[B, T <: B, F <: B] <: B
    }

    // Implements abstract type in concrete Types
    trait True extends Bool {
      type &&[B <: Bool] = B
      type ||[B <: Bool] = True
      type IfElse[B, T <: B, F <: B] = T
    }
    object True extends True // Mixin trait with object avoids
                             // `True.type`

    trait False extends Bool {
      type &&[B <: Bool] = False
      type ||[B <: Bool] = B
      type IfElse[B, T <: B, F <: B] = F
    }
    object False extends False

    object Test {
      implicitly[ (False#  &&  [False]) =:= False ]
      implicitly[ (False#  &&  [True])  =:= False ]
      implicitly[ (True#   &&  [False]) =:= False ]
      implicitly[ (True#   &&  [True])  =:= True  ]

      implicitly[ (False#  ||  [False]) =:= False ]
      implicitly[ (False#  ||  [True])  =:= True  ]
      implicitly[ (True#   ||  [False]) =:= True  ]
      implicitly[ (True#   ||  [True])  =:= True  ]

      implicitly[ False#IfElse[Any, Int, String]  =:= String ]
      implicitly[ True# IfElse[Any, Int, String]  =:= Int    ]
    }
  }

  //----------------------------------------------------- Natural Numbers
  object NatTermLevel {
    sealed trait Nat {
      def ++ : Nat = new Succ(this)
    }

    final case object Zero extends Nat

    final case class Succ(n: Nat) extends Nat

    // Peano Numbers
    val _0 = Zero
    val _1 = _0.++
    val _2 = _1.++

    object Test {
      import TypeLevelComputation.BooleanTermLevel._

      assert ( _0    ==            Zero   )
      assert ( _1    ==       Succ(Zero)  )
      assert ( _1.++ ==  Succ(Succ(Zero)) )
      assert ( _2    ==  Succ(Succ(Zero)) )

      assert ( False.ifElse (_0, _1) == _0 )
      assert ( True.ifElse  (_0, _1) == _1 )
    }
  }

  object NatTypeLevel {
    sealed trait Nat {
      // For ++ operation, using `this.type` doesn't hold. `this.type`
      // reduces Zero type to `Zero.type` and Succ[N] to `_ <:
      // Succ[N]`. Thus, the test `implicitly[ _1 # ++ =:=
      // Succ[Succ[Zero]] ]` doesn't hold because the type system
      // cannot prove that `Succ[_ <: Succ[Zero.type] with Singleton]
      // =:= Succ[Succ[Zero]]`.
      //
      // type ++ = Succ[this.type]
      //
      // We have to help the compilator giving explicit reduction
      // rule, here using `This`.
      type This >: this.type <: Nat
      type ++ = Succ[This]
    }

    final object Zero extends Nat {
      type This = Zero
    }
    type Zero = Zero.type // Alternative to Mixin trait with object

    final class Succ[N <: Nat] extends Nat {
      type This = Succ[N]
    }

    // Peano Numbers:
    type _0 = Zero
    type _1 = _0 # ++
    type _2 = _1 # ++

    object Test {
      import TypeLevelComputation.BooleanTypeLevel._

      implicitly[ _0      =:=            Zero   ]
      implicitly[ _0 # ++ =:=       Succ[Zero]  ]
      implicitly[ _1      =:=       Succ[Zero]  ]
      implicitly[ _1 # ++ =:=  Succ[Succ[Zero]] ]
      implicitly[ _2      =:=  Succ[Succ[Zero]] ]
      implicitly[ _2      =:=  Succ[    _1    ] ]

      implicitly[ False#IfElse[Nat, _0, _1]  =:= _1 ]
      implicitly[ True# IfElse[Nat, _0, _1]  =:= _0  ]
    }
  }

  /*----------------------------------------------------------*/
  /* Translation Rules                                        */
  /*                                                          */
  /* ADT (Algebraic Data Types) Values: val => object         */
  /* Members: def x/ val x                  => type X         */
  /*          def f(x)                      => type F[X]      */
  /* a.b                                    => A#B            */
  /* x: T                                   => X <: T         */
  /* new A(b)                               => A[B]           */
  /*----------------------------------------------------------*/

  //------------------------------------------------------------ Recursion
  object NatTermLevelRecurs {
    sealed trait Nat {
      def ++ : Nat = new Succ(this)
      def +(x: Nat): Nat
    }

    final case object Zero extends Nat {
      def +(x: Nat): Nat = x
    }

    final case class Succ(n: Nat) extends Nat {
      def +(x: Nat): Nat = Succ(n + x)
    }

    val _0 = Zero
    val _1 = _0.++
    val _2 = _1.++
    val _3 = _1 + _2

    object Test {
      assert ( _3           ==  _3 )
      assert ( _1 + _1 + _1 ==  _3 )
      assert ( _1.++ + _1   ==  _3 )
    }
  }

  object NatTypeLevelRecurs {
    sealed trait Nat {
      type This >: this.type <: Nat
      type ++ = Succ[This]
      type +[_ <: Nat] <: Nat
    }

    final object Zero extends Nat {
      type This = Zero
      type +[X <: Nat] = X
    }
    type Zero = Zero.type

    final class Succ[N <: Nat] extends Nat {
      type This = Succ[N]
      type +[X <: Nat] = Succ[N # + [X]]
    }

    type _0 = Zero
    type _1 = _0 # ++
    type _2 = _1 # ++
    type _3 = _1 # + [_2]

    object Test {
      implicitly[ _1 # ++ # + [_1] =:= _3 ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= Succ[Succ[Succ[Zero]]] ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= _3 ]
    }
  }

  // ------------------------------------------------------ Type Functions
  object NatTermLevelTypeFunc {
    sealed trait Nat {
      def ++ : Nat = new Succ(this)
      // Fold implementation
      def fold[U](f: U => U, z: => U): U // Church Numerals, `z` is
                                         // our accumulator
      // Here, we implement `+` for all instance of Nat using `fold`.
      def +(x: Nat): Nat =
        fold[Nat]((n: Nat) => Succ(n), x)
    }

    final case object Zero extends Nat {
      def fold[U](f: U => U, z: => U) = z
    }

    final case class Succ(n: Nat) extends Nat {
      def fold[U](f: U => U, z: => U) = f(n.fold(f, z))
    }

    val _0 = Zero
    val _1 = _0.++
    val _2 = _1.++
    val _3 = _1 + _2

    object Test {
      assert ( _3           ==  _3 )
      assert ( _1 + _1 + _1 ==  _3 )
      assert ( _1.++ + _1   ==  _3 )
    }
  }

  object NatTypeLevelTypeFunc {
    sealed trait Nat {
      type This >: this.type <: Nat
      type ++ = Succ[This]

      /** Type Function */
      type Fold[U, // fold[U](
                F[_ <: U] <: U, // f: U => U
                Z <: U // z: => U
               ] <: U // ): U

      /** Type Lamdba
        *
        * {{{
        * def +(x: Nat): Nat =
        *   fold[Nat]((n: Nat) => Succ(n), x)
        * }}}
        *
        * {{{
        * def +(x: Nat): Nat =
        *   fold[Nat](  (new { def λ(n: Nat) = Succ(n) }).λ _ // lambda,
        *                x   )
        */
      type +[X <: Nat] =
        Fold[Nat, ({ type λ[N <: Nat] = Succ[N] })#λ, X]
        // Fold[Nat, [N <: Nat] => Succ[N], X] // Experimental
    }

    final object Zero extends Nat {
      type This = Zero
      type Fold[U, F[_ <: U] <: U, Z <: U] = Z
    }
    type Zero = Zero.type

    final class Succ[N <: Nat] extends Nat {
      type This = Succ[N]
      type Fold[U, F[_ <: U] <: U, Z <: U] = F[N#Fold[U, F, Z]]
    }

    type _0 = Zero
    type _1 = _0 # ++
    type _2 = _1 # ++
    type _3 = _1 # + [_2]

    object Test {
      implicitly[ _1 # ++ # + [_1] =:= _3 ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= Succ[Succ[Succ[Zero]]] ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= _3 ]
    }
  }

  // ------------------------------------ Term- And Type-Level Combination
  object NatTermAndTypeLevel {
    sealed trait Nat {
      type This >: this.type <: Nat
      type Fold[U, F[_ <: U] <: U, Z <: U] <: U
      type ++ = Succ[This]

      // Combining Term- and Type-Level
      def value: Int

      type +[X <: Nat] =
        Fold[Nat, ({ type λ[N <: Nat] = Succ[N] })#λ, X]
      // Hybrid operation: Term- & Type-level. Thus our `+` method is
      // correctly typed.
      def  +[X <: Nat](n: X): +[X] =
        // Here, we have to wrap the int operation in
        // the correct Nat.
        Nat._unsafe[+[X]](value + n.value)

      def ++ : ++ = Succ(this.value)

      // -- For HList # Span
      type GT_0[B, T <: B, F <: B] <: B
      type -- <: Nat
    }
    object Nat {
      def _unsafe[N <: Nat](v: Int) =
        (if (v == 0) Zero else Succ(v)).asInstanceOf[N]
    }

    final object Zero extends Nat {
      type This = Zero
      type Fold[U, F[_ <: U] <: U, Z <: U] = Z

      def value = 0

      // -- For HList # Span
      type GT_0[B, T <: B, F <: B] = F
      type -- = Nothing
    }
    type Zero = Zero.type

    final case class Succ[N <: Nat](val value: Int) extends Nat {
      type This = Succ[N]
      type Fold[U, F[_ <: U] <: U, Z <: U] = F[N#Fold[U, F, Z]]

      // -- For HList # Span
      type GT_0[B, T <: B, F <: B] = T
      type -- = N
    }

    type _0 = Zero
    type _1 = _0 # ++
    type _2 = _1 # ++
    type _3 = _1 # + [_2]

    val _0: _0 = Zero
    val _1: _1 = Succ[_0](_0.value)
    val _2: _2 = Succ[_1](_1.value)
    val _3: _3 = Succ[_2](_2.value)

    object Test {
      implicitly[ _1 # ++ # + [_1]        =:= _3 ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= Succ[Succ[Succ[Zero]]] ]
      implicitly[ _1 # + [ _1 # + [ _1 ]] =:= _3 ]

      implicitly[ _1 # GT_0[Nat, _1, _0]                           =:= _1 ]
      implicitly[ _0 # GT_0[Nat, _1, _0]                           =:= _0 ]
      implicitly[ _0 # GT_0[(Nat,Nat,Nat), (_1,_1,_1), (_2,_2,_2)] =:= (_2,_2,_2) ]

      implicitly[ _0 # --           =:= Nothing ]
      implicitly[ _1 # --           =:= _0 ]
      implicitly[ _2 # -- # --      =:= _0 ]
      implicitly[ _2 # -- # -- # -- =:= Nothing ]

      assert ( _1 + _1 + _1 == _3 )
      assert ( _1 + _2      == _3 )
    }
  }

  // ------------------------------------------------- Heterogeneous Lists
  object HeterogeneousList {
    import NatTermAndTypeLevel._

    // REDFLAG: Always put inner used type outer!
    trait Sp {
      type T1
      type T2 <: HList
      type T3 <: HList

      type Out

      def _1: T1
      def _2: T2
      def _3: T3
    }

    trait INatSp extends Sp {
      type T1 <: Nat
      type Out <: (T2, T3)
    }

    trait NatSp[N <: Nat,
                P <: HList,
                S <: HList] extends INatSp {
      type T1 = N
      type T2 = P
      type T3 = S

      type Out = (T2, T3)
    }


    trait IIntSp extends Sp {
      type T1 <: Int
      def res: (T2, T3) = (_2, _3)
    }

    case class IntSp[P <: HList, S <: HList](val _1: Int,
                                             val _2: P,
                                             val _3: S) extends IIntSp {
      type T1 = Int
      type T2 = P
      type T3 = S
    }

    sealed abstract class HList {
      type This >: this.type <: HList
      type Head
      type Tail <: HList

      def head: Head
      def tail: Tail

      // HList Length
      def length: Length = Nat._unsafe[Length](fold((_, n: Int) => n + 1, 0))
      type Length = Fold[Nat, ({ type λ[_, N <: Nat] = N # ++ })#λ, Zero]

      // HList Span
      def span1: Span1 = (HCons(head, HNil), tail)
      type Span1 = (HCons[Head, HNil.type], Tail)

      def span2: Span2 = (HCons(head, HCons(tail.head, HNil)), tail.tail)
      type Span2 = (HCons[Head, HCons[Tail#Head, HNil.type]], Tail#Tail)

      def span3: Span3 =
        (HCons(head, HCons(tail.head, HCons(tail.tail.head, HNil))), tail.tail.tail)
      type Span3 =
        (HCons[Head, HCons[Tail#Head, HCons[Tail#Tail#Head, HNil.type]]], Tail#Tail#Tail)

      // FIXME
      /*
       import TypeLevelComputation.NatTermAndTypeLevel._
       import TypeLevelComputation.HeterogeneousList._
       HCons(1, HCons("1", HNil)).span(_1)
       */
      def span[N <: Nat](n: N)/*: SpanN[N]#Out*/ =
        (fold[IIntSp](
           { case (h, IntSp(n,p,s)) => if (n > 0) IntSp(n-1, HCons(h, p), s.tail)
                                       else IntSp(0, p, s)
           }, IntSp(n.value, HNil, this)).res)/*.asInstanceOf[SpanN[N] # Out]*/


      implicitly[ _1 # GT_0[HList, HCons[Int,HNil], HNil]
                  =:= HCons[Int,HNil] ]
      implicitly[ _0 # GT_0[HList, HCons[Int,HNil], HNil]
                  =:= HNil ]
      implicitly[ _1 # GT_0[INatSp, NatSp[_1,HNil,HNil], NatSp[_0,HNil,HNil]]
                  =:= NatSp[_1,HNil,HNil] ]
      implicitly[ _0 # GT_0[INatSp, NatSp[_1,HNil,HNil], NatSp[_0,HNil,HNil]]
                  =:= NatSp[_0,HNil,HNil] ]
      implicitly[ _1 # GT_0[INatSp,
                            NatSp[_1,HCons[Int,HNil],HNil],
                            NatSp[_0,HCons[Int,HNil],HNil]]
                  =:=  NatSp[_1,HCons[Int,HNil],HNil] ]
      implicitly[ _0 # GT_0[INatSp,
                            NatSp[_1,HCons[Int,HNil],HNil],
                            NatSp[_0,HCons[Int,HNil],HNil]]
                  =:=  NatSp[_0,HCons[Int,HNil],HNil] ]

      implicitly[ NatSp[_0,HCons[Int,HNil],HNil]#T1                 =:= _0 ]
      implicitly[ NatSp[_0,HCons[Int,HNil],HNil]#T2                 =:= HCons[Int,HNil] ]
      implicitly[ NatSp[_0,HCons[Int,HNil],HNil]#T3                 =:= HNil ]
      implicitly[ NatSp[_0,HCons[Int,HNil],HNil]#T1#GT_0[Nat,_1,_0] =:= _0 ]
      implicitly[ NatSp[_1,HCons[Int,HNil],HNil]#T1#GT_0[Nat,_1,_0] =:= _1 ]
      implicitly[ NatSp[_2,HCons[Int,HNil],HNil]#T1#GT_0[Nat,_1,_0] =:= _1 ]


      //* FIXME:
      type SpanN[X <: Nat] = Fold[INatSp,
                                  ({ type λ[H, T <: INatSp] =
                                      T # T1 # GT_0[INatSp,
                                                    NatSp[T # T1 # --,
                                                          HCons[H, T # T2],
                                                          T # T3 # Tail],
                                                    NatSp[_0, T # T2, T # T3]]
                                   })#λ, NatSp[X,HNil,This]]

      implicitly[ HNil # SpanN[_0] =:= NatSp[_0, HNil, HNil] ]
      implicitly[ HNil # SpanN[_1] =:= NatSp[_1, HNil, HNil] ]
      implicitly[ HNil # SpanN[_2] =:= NatSp[_2, HNil, HNil] ]

      implicitly[ HCons[Int,HNil] # SpanN[_0] =:= NatSp[_0, HNil, HCons[Int,HNil]] ]
      implicitly[ HCons[Int,HNil] # SpanN[_1] =:= NatSp[_0, HCons[Int,HNil], HNil] ]
      implicitly[ HCons[Int,HNil] # SpanN[_2] =:= NatSp[_1, HCons[Int,HNil], HNil] ]

      implicitly[ HCons[Int,HCons[String,HNil]] # SpanN[_0]
                   =:= NatSp[_0, HNil, HCons[Int,HCons[String,HNil]]] ]
      // BUG: Why String, String in result?
      implicitly[ HCons[Int,HCons[String,HNil]] # SpanN[_1]
                   =:= NatSp[_0, HCons[String,HNil], HCons[String,HNil]] ]

      // val a: HCons[Int,HCons[String,HCons[Char,HNil]]] # SpanN[_1] = "a"

      // */

      def fold[U](f: (Any, U) => U, z: => U): U // f is Function2
      type Fold[U, F[_, _ <: U] <: U, Z <: U] <: U
    }

    final case class HCons[H, T <: HList](val head: H,
                                          val tail: T) extends HList {
      type This = HCons[H,T]
      type Head = H
      type Tail = T

      // Utils
      def fold[U](f: (Any, U) => U, z: => U) = f(head, tail.fold[U](f, z))
      type Fold[U, F[_, _ <: U] <: U, Z <: U] = F[Head, T#Fold[U, F, Z]]
    }

    final object HNil extends HList {
      type This = HNil
      type Head = Nothing
      type Tail = Nothing

      def head = throw new NoSuchElementException("HNil.head")
      def tail = throw new NoSuchElementException("HNil.tail")

      // Utils
      def fold[U](f: (Any, U) => U, z: => U) = z
      type Fold[U, F[_, _ <: U] <: U, Z <: U] = Z
    }
    type HNil = HNil.type

    assert ( HCons("a", HCons(1, HNil)).length  ==  _2 )
    assert ( HCons(1, HCons("1", HNil)).span(_1) == (HCons(1, HNil), HCons("1", HNil)) )

    object Test {
      // val t:  (HCons[Int, HNil], HCons[String, HNil]) =
      //   HCons(1, HCons("1", HNil)).span(_1)
    }
  }
}
