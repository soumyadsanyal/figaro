/*
 * VEBPChooserTest.scala
 * Test of hybrid VE/BP problem solver.
 *
 * Created By:      Avi Pfeffer (apfeffer@cra.com)
 * Creation Date:   March 1, 2015
 *
 * Copyright 2017 Avrom J. Pfeffer and Charles River Analytics, Inc.
 * See http://www.cra.com or email figaro@cra.com for information.
 *
 * See http://www.github.com/p2t2/figaro for a copy of the software license.
 */
package com.cra.figaro.test.algorithm.structured.solver

import org.scalatest.{WordSpec, Matchers}
import com.cra.figaro.language._
import com.cra.figaro.library.compound._
import com.cra.figaro.algorithm.factored.factors.Factor
import com.cra.figaro.algorithm.factored.factors.factory.Factory
import com.cra.figaro.algorithm.factored.factors.SumProductSemiring
import com.cra.figaro.algorithm.lazyfactored.Regular
import com.cra.figaro.algorithm.structured.strategy.solve._
import com.cra.figaro.algorithm.structured._
import com.cra.figaro.algorithm.structured.solver._

class VEBPChooserTest extends WordSpec with Matchers {
  "Choosing VE or BP with a high score threshold so VE is chosen" when {
    "given a flat model with no conditions or constraints" should {
      "produce the correct result over a single element" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        val pr = new Problem(cc, List(e2))
        pr.add(e1)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal (Set(c2))
        pr.solved should equal (true)
        val result = multiplyAll(pr.solution)
        result.variables should equal (List(c2.variable))
        result.size should equal (2)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        result.get(List(c2IndexT)) should be (0.6 +- 0.00000001)
        result.get(List(c2IndexF)) should be (0.4 +- 0.00000001)
      }

      "produce the correct result over multiple elements" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        val pr = new Problem(cc, List(e2, e3))
        pr.add(e1)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal (Set(c2, c3))
        val result = multiplyAll(pr.solution)
        result.variables.size should equal (2)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        val c3IndexT = c3.variable.range.indexOf(Regular(true))
        val c3IndexF = c3.variable.range.indexOf(Regular(false))
        result.size should equal (4)
        val var0 = result.variables(0)
        val var1 = result.variables(1)
        if (var0 == c2.variable) {
          var1 should equal (c3.variable)
          result.get(List(c2IndexT, c3IndexT)) should equal (0.6)
          result.get(List(c2IndexT, c3IndexF)) should equal (0.0)
          result.get(List(c2IndexF, c3IndexT)) should equal (0.0)
          result.get(List(c2IndexF, c3IndexF)) should equal (0.4)
        } else {
          var0 should equal (c3.variable)
          var1 should equal (c2.variable)
          result.get(List(c3IndexT, c2IndexT)) should equal (0.6)
          result.get(List(c3IndexT, c2IndexF)) should equal (0.0)
          result.get(List(c3IndexF, c2IndexT)) should equal (0.0)
          result.get(List(c3IndexF, c2IndexF)) should equal (0.4)
        }
      }
    }

    "given a condition on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.observe(true)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        result.get(List(c1Index3)) should be ((0.25 * 0.3) +- 0.000000001)
        result.get(List(c1Index5)) should be ((0.25 * 0.5) +- 0.000000001)
        result.get(List(c1Index7)) should be ((0.25 * 0.7) +- 0.000000001)
        result.get(List(c1Index9)) should be ((0.25 * 0.9) +- 0.000000001)
      }
    }

    "given a constraint on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        result.get(List(c1Index3)) should be ((0.25 * (0.3 * 0.5 + 0.7 * 0.2)) +- 0.000000001)
        result.get(List(c1Index5)) should be ((0.25 * (0.5 * 0.5 + 0.5 * 0.2)) +- 0.000000001)
        result.get(List(c1Index7)) should be ((0.25 * (0.7 * 0.5 + 0.3 * 0.2)) +- 0.000000001)
        result.get(List(c1Index9)) should be ((0.25 * (0.9 * 0.5 + 0.1 * 0.2)) +- 0.000000001)
      }
    }

    "given two constraints on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        e3.addConstraint((b: Boolean) => if (b) 0.4 else 0.1)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        result.get(List(c1Index3)) should be ((0.25 * (0.3 * 0.5 * 0.4 + 0.7 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index5)) should be ((0.25 * (0.5 * 0.5 * 0.4 + 0.5 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index7)) should be ((0.25 * (0.7 * 0.5 * 0.4 + 0.3 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index9)) should be ((0.25 * (0.9 * 0.5 * 0.4 + 0.1 * 0.2 * 0.1)) +- 0.000000001)
      }
    }

    "given constraints on two dependent elements" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e2.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        e3.addConstraint((b: Boolean) => if (b) 0.4 else 0.1)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        result.get(List(c1Index3)) should be ((0.25 * (0.3 * 0.5 * 0.4 + 0.7 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index5)) should be ((0.25 * (0.5 * 0.5 * 0.4 + 0.5 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index7)) should be ((0.25 * (0.7 * 0.5 * 0.4 + 0.3 * 0.2 * 0.1)) +- 0.000000001)
        result.get(List(c1Index9)) should be ((0.25 * (0.9 * 0.5 * 0.4 + 0.1 * 0.2 * 0.1)) +- 0.000000001)
      }
    }

    "given a contingent condition on an element" should {
      "produce the result with the correct probability" in {
        val universe = Universe.createNew()
        val cc = new ComponentCollection
        val ec1 = new EC1
        val ec2 = new EC1
        val e11 = Flip(0.6)("e1", ec1)
        val e12 = Flip(0.3)("e1", ec2)
        val e2 = Select(0.8 -> ec1, 0.2 -> ec2)("e2", universe)
        universe.assertEvidence("e2.e1", Observation(true))
        val pr = new Problem(cc, List(e2))
        pr.add(e11)
        pr.add(e12)
        val c11 = cc(e11)
        val c12 = cc(e12)
        val c2 = cc(e2)
        c11.generateRange()
        c12.generateRange()
        c2.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        pr.globals should equal (Set(c2))
        val result = multiplyAll(pr.solution)
        val c2Index1 = c2.variable.range.indexOf(Regular(ec1))
        val c2Index2 = c2.variable.range.indexOf(Regular(ec2))
        result.size should equal (2)
        result.get(List(c2Index1)) should be ((0.8 * 0.6) +- 0.000000001)
        result.get(List(c2Index2)) should be ((0.2 * 0.3) +- 0.000000001)
      }
    }

    "with an element that uses another element multiple times, " +
      "always produce the same value for the different uses" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Flip(0.5)
        val e2 = Apply(e1, e1, (b1: Boolean, b2: Boolean) => b1 == b2)
        val pr = new Problem(cc, List(e2))
        pr.add(e1)
        val c1 = cc(e1)
        val c2 = cc(e2)
        c1.generateRange()
        c2.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

        val result = multiplyAll(pr.solution)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        result.get(List(c2IndexT)) should be (1.0 +- 0.000000001)
        result.get(List(c2IndexF)) should be (0.0 +- 0.000000001)
      }

    "with a constraint on an element that is used multiple times, only factor in the constraint once" in {
      Universe.createNew()
      val cc = new ComponentCollection
      val f1 = Flip(0.5)
      val f2 = Flip(0.3)
      val e1 = Apply(f1, f1, (b1: Boolean, b2: Boolean) => b1 == b2)
      val e2 = Apply(f1, f2, (b1: Boolean, b2: Boolean) => b1 == b2)
      val d = Dist(0.5 -> e1, 0.5 -> e2)
      f1.setConstraint((b: Boolean) => if (b) 3.0; else 2.0)

      val pr = new Problem(cc, List(d))
      pr.add(f1)
      pr.add(f2)
      pr.add(e1)
      pr.add(e2)
      val cf1 = cc(f1)
      val cf2 = cc(f2)
      val ce1 = cc(e1)
      val ce2 = cc(e2)
      val cd = cc(d)
      cf1.generateRange()
      cf2.generateRange()
      ce1.generateRange()
      ce2.generateRange()
      cd.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      // Probability that f1 is true = 0.6
      // Probability that e1 is true = 1.0
      // Probability that e2 is true = 0.6 * 0.3 + 0.4 * 0.7 = 0.46
      // Probability that d is true = 0.5 * 1 + 0.5 * 0.46 = 0.73
      val result = multiplyAll(pr.solution)
      val dIndexT = cd.variable.range.indexOf(Regular(true))
      val dIndexF = cd.variable.range.indexOf(Regular(false))
      val pT = result.get(List(dIndexT))
      val pF = result.get(List(dIndexF))
      (pT / (pT + pF)) should be (0.73 +- 0.000000001)
    }

    "with elements that are not used by the query or evidence, produce the correct result" in {
      val u1 = Universe.createNew()
      val cc = new ComponentCollection
      val u = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
      val f = Flip(u)
      val a = If(f, Select(0.3 -> 1, 0.7 -> 2), Constant(2))
      val pr = new Problem(cc, List(f))
      pr.add(u)
      pr.add(a)
      val cu = cc(u)
      val cf = cc(f)
      val ca = cc(a)
      cu.generateRange()
      cf.generateRange()
      ca.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val fIndexT = cf.variable.range.indexOf(Regular(true))
      val fIndexF = cf.variable.range.indexOf(Regular(false))
      val pT = result.get(List(fIndexT))
      val pF = result.get(List(fIndexF))
      (pT / (pT + pF)) should be (0.6 +- 0.000000001)
    }

    "with a model using chain and no conditions or constraints, when the outcomes are at the top level, produce the correct answer" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      pr.add(e2)
      pr.add(e3)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      result.get(List(c4Index1)) should be ((0.3 * 0.1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and no conditions or constraints, when the outcomes are nested, produce the correct answer" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      result.get(List(c4Index1)) should be ((0.3 * 0.1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on the result, when the outcomes are at the top level, correctly condition the parent" in {
      Universe.createNew()
      val e1= Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e4.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e2)
      pr.add(e3)
      pr.add(e4)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be ((0.3 * 0.1 / (0.3 * 0.1 + 0.7 * 0.7)) +- 0.000000001)
    }

    "with a model using chain and a condition on the result, when the outcomes are nested, correctly condition the parent" in {
      Universe.createNew()
      val e1= Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e4.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e4)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be ((0.3 * 0.1 / (0.3 * 0.1 + 0.7 * 0.7)) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are at the top level, correctly condition the result" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      pr.add(e2)
      pr.add(e3)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      val c4Index2 = c4.variable.range.indexOf(Regular(2))
      val c4Index3 = c4.variable.range.indexOf(Regular(3))
      val p1 = result.get(List(c4Index1))
      val p2 = result.get(List(c4Index2))
      val p3 = result.get(List(c4Index3))
      (p1 / (p1 + p2 + p3)) should be ((0.3 * 1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are at the top level, " +
    "not change the belief about the parent" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e2)
      pr.add(e3)
      pr.add(e4)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be (0.3 +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are nested, correctly condition the result" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      pr.add(e2)
      pr.add(e3)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      val c4Index2 = c4.variable.range.indexOf(Regular(2))
      val c4Index3 = c4.variable.range.indexOf(Regular(3))
      val p1 = result.get(List(c4Index1))
      val p2 = result.get(List(c4Index2))
      val p3 = result.get(List(c4Index3))
      (p1 / (p1 + p2 + p3)) should be ((0.3 * 1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are nested, " +
    "not change the belief about the parent" in {
      Universe.createNew()

      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e2)
      pr.add(e3)
      pr.add(e4)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.PositiveInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be (0.3 +- 0.000000001)
    }
  }

  "Choosing VE or BP with a low score threshold so BP is chosen with an acyclic model" when {

    "given a flat model with no conditions or constraints" should {
      "produce the correct result over a single element" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        val pr = new Problem(cc, List(e2))
        pr.add(e1)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal (Set(c2))
        pr.solved should equal (true)
        val result = multiplyAll(pr.solution)
        result.variables should equal (List(c2.variable))
        result.size should equal (2)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        result.get(List(c2IndexT)) should be (0.6 +- 0.00000001)
        result.get(List(c2IndexF)) should be (0.4 +- 0.00000001)
      }

      "produce the correct result over multiple elements" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        val pr = new Problem(cc, List(e2, e3))
        pr.add(e1)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal (Set(c2, c3))
        val result = multiplyAll(pr.solution)
        result.variables.size should equal (2)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        val c3IndexT = c3.variable.range.indexOf(Regular(true))
        val c3IndexF = c3.variable.range.indexOf(Regular(false))
        result.size should equal (4)
        val var0 = result.variables(0)
        val var1 = result.variables(1)
        if (var0 == c2.variable) {
          var1 should equal(c3.variable)
          // Note the answers are incorrect, but since the model is loopy now we can't guarantee the answer. This check is to ensure
          // that any subsequent changes to BP that change this value should be noted
          result.get(List(c2IndexT, c3IndexT)) should equal(0.36 +- 0.00001) // should be 0.6
          result.get(List(c2IndexT, c3IndexF)) should equal(0.24 +- 0.00001) // should be 0
          result.get(List(c2IndexF, c3IndexT)) should equal(0.24 +- 0.00001) // 0
          result.get(List(c2IndexF, c3IndexF)) should equal(0.16 +- 0.00001) // .16
        } else {
          var0 should equal(c3.variable)
          var1 should equal(c2.variable)
          // Note the answers are incorrect, but since the model is loopy now we can't guarantee the answer. This check is to ensure
          // that any subsequent changes to BP that change this value should be noted
          result.get(List(c3IndexT, c2IndexT)) should equal(0.36 +- 0.00001) // should be 0.6
          result.get(List(c3IndexT, c2IndexF)) should equal(0.24 +- 0.00001) // should be 0
          result.get(List(c3IndexF, c2IndexT)) should equal(0.24 +- 0.00001) // 0
          result.get(List(c3IndexF, c2IndexF)) should equal(0.16 +- 0.00001) // .16
        }
      }
    }

    "given a condition on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.observe(true)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        val x3 = 0.25 * 0.3
        val x5 = 0.25 * 0.5
        val x7 = 0.25 * 0.7
        val x9 = 0.25 * 0.9
        val z = x3 + x5 + x7 + x9
        result.get(List(c1Index3)) should be ((x3 / z) +- 0.000000001)
        result.get(List(c1Index5)) should be ((x5 / z) +- 0.000000001)
        result.get(List(c1Index7)) should be ((x7 / z) +- 0.000000001)
        result.get(List(c1Index9)) should be ((x9 / z) +- 0.000000001)
      }
    }

    "given a constraint on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        val x3 = 0.25 * (0.3 * 0.5 + 0.7 * 0.2)
        val x5 = 0.25 * (0.5 * 0.5 + 0.5 * 0.2)
        val x7 = 0.25 * (0.7 * 0.5 + 0.3 * 0.2)
        val x9 = 0.25 * (0.9 * 0.5 + 0.1 * 0.2)
        val z = x3 + x5 + x7 + x9
        result.get(List(c1Index3)) should be (x3 / z +- 0.000000001)
        result.get(List(c1Index5)) should be (x5 / z +- 0.000000001)
        result.get(List(c1Index7)) should be (x7 / z +- 0.000000001)
        result.get(List(c1Index9)) should be (x9 / z +- 0.000000001)
      }
    }

    "given two constraints on a dependent element" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e3.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        e3.addConstraint((b: Boolean) => if (b) 0.4 else 0.1)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        val x3 = 0.25 * (0.3 * 0.5 * 0.4 + 0.7 * 0.2 * 0.1)
        val x5 = 0.25 * (0.5 * 0.5 * 0.4 + 0.5 * 0.2 * 0.1)
        val x7 = 0.25 * (0.7 * 0.5 * 0.4 + 0.3 * 0.2 * 0.1)
        val x9 = 0.25 * (0.9 * 0.5 * 0.4 + 0.1 * 0.2 * 0.1)
        val z = x3 + x5 + x7 + x9
        result.get(List(c1Index3)) should be (x3 / z +- 0.000000001)
        result.get(List(c1Index5)) should be (x5 / z +- 0.000000001)
        result.get(List(c1Index7)) should be (x7 / z +- 0.000000001)
        result.get(List(c1Index9)) should be (x9 / z +- 0.000000001)
      }
    }

    "given constraints on two dependent elements" should {
      "produce the result with the correct probability" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
        val e2 = Flip(e1)
        val e3 = Apply(e2, (b: Boolean) => b)
        e2.addConstraint((b: Boolean) => if (b) 0.5 else 0.2)
        e3.addConstraint((b: Boolean) => if (b) 0.4 else 0.1)
        val pr = new Problem(cc, List(e1))
        pr.add(e2)
        pr.add(e3)
        val c1 = cc(e1)
        val c2 = cc(e2)
        val c3 = cc(e3)
        c1.generateRange()
        c2.generateRange()
        c3.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal  (Set(c1))
        val result = multiplyAll(pr.solution)
        val c1Index3 = c1.variable.range.indexOf(Regular(0.3))
        val c1Index5 = c1.variable.range.indexOf(Regular(0.5))
        val c1Index7 = c1.variable.range.indexOf(Regular(0.7))
        val c1Index9 = c1.variable.range.indexOf(Regular(0.9))
        result.size should equal (4)
        val x3 = 0.25 * (0.3 * 0.5 * 0.4 + 0.7 * 0.2 * 0.1)
        val x5 = 0.25 * (0.5 * 0.5 * 0.4 + 0.5 * 0.2 * 0.1)
        val x7 = 0.25 * (0.7 * 0.5 * 0.4 + 0.3 * 0.2 * 0.1)
        val x9 = 0.25 * (0.9 * 0.5 * 0.4 + 0.1 * 0.2 * 0.1)
        val z = x3 + x5 + x7 + x9
        result.get(List(c1Index3)) should be (x3 / z +- 0.000000001)
        result.get(List(c1Index5)) should be (x5 / z +- 0.000000001)
        result.get(List(c1Index7)) should be (x7 / z +- 0.000000001)
        result.get(List(c1Index9)) should be (x9 / z +- 0.000000001)
      }
    }

    "given a contingent condition on an element" should {
      "produce the result with the correct probability" in {
        val universe = Universe.createNew()
        val cc = new ComponentCollection
        val ec1 = new EC1
        val ec2 = new EC1
        val e11 = Flip(0.6)("e1", ec1)
        val e12 = Flip(0.3)("e1", ec2)
        val e2 = Select(0.8 -> ec1, 0.2 -> ec2)("e2", universe)
        universe.assertEvidence("e2.e1", Observation(true))
        val pr = new Problem(cc, List(e2))
        pr.add(e11)
        pr.add(e12)
        val c11 = cc(e11)
        val c12 = cc(e12)
        val c2 = cc(e2)
        c11.generateRange()
        c12.generateRange()
        c2.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

        pr.globals should equal (Set(c2))
        val result = multiplyAll(pr.solution)
        val c2Index1 = c2.variable.range.indexOf(Regular(ec1))
        val c2Index2 = c2.variable.range.indexOf(Regular(ec2))
        result.size should equal (2)
        val x1 = (0.8 * 0.6)
        val x2 = (0.2 * 0.3)
        val z = x1 + x2
        result.get(List(c2Index1)) should be ((x1 / z) +- 0.000000001)
        result.get(List(c2Index2)) should be ((x2 / z) +- 0.000000001)
      }
    }

    "with an element that uses another element multiple times, " +
      "always produce the same value for the different uses" in {
        Universe.createNew()
        val cc = new ComponentCollection
        val e1 = Flip(0.5)
        val e2 = Apply(e1, e1, (b1: Boolean, b2: Boolean) => b1 == b2)
        val pr = new Problem(cc, List(e2))
        pr.add(e1)
        val c1 = cc(e1)
        val c2 = cc(e2)
        c1.generateRange()
        c2.generateRange()
        new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()
        val result = multiplyAll(pr.solution)
        val c2IndexT = c2.variable.range.indexOf(Regular(true))
        val c2IndexF = c2.variable.range.indexOf(Regular(false))
        result.get(List(c2IndexT)) should be (1.0 +- 0.000000001)
        result.get(List(c2IndexF)) should be (0.0 +- 0.000000001)
      }

    "with a constraint on an element that is used multiple times, only factor in the constraint once" in {
      Universe.createNew()
      val cc = new ComponentCollection
      val f1 = Flip(0.5)
      val f2 = Flip(0.3)
      val e1 = Apply(f1, f1, (b1: Boolean, b2: Boolean) => b1 == b2)
      val e2 = Apply(f1, f2, (b1: Boolean, b2: Boolean) => b1 == b2)
      val d = Dist(0.5 -> e1, 0.5 -> e2)
      f1.setConstraint((b: Boolean) => if (b) 3.0; else 2.0)

      val pr = new Problem(cc, List(d))
      pr.add(f1)
      pr.add(f2)
      pr.add(e1)
      pr.add(e2)
      val cf1 = cc(f1)
      val cf2 = cc(f2)
      val ce1 = cc(e1)
      val ce2 = cc(e2)
      val cd = cc(d)
      cf1.generateRange()
      cf2.generateRange()
      ce1.generateRange()
      ce2.generateRange()
      cd.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      // Probability that f1 is true = 0.6
      // Probability that e1 is true = 1.0
      // Probability that e2 is true = 0.6 * 0.3 + 0.4 * 0.7 = 0.46
      // Probability that d is true = 0.5 * 1 + 0.5 * 0.46 = 0.73
      val result = multiplyAll(pr.solution)
      val dIndexT = cd.variable.range.indexOf(Regular(true))
      val dIndexF = cd.variable.range.indexOf(Regular(false))
      val pT = result.get(List(dIndexT))
      val pF = result.get(List(dIndexF))
      (pT / (pT + pF)) should be (0.73 +- 0.000000001)
    }

    "with elements that are not used by the query or evidence, produce the correct result" in {
      val u1 = Universe.createNew()
      val cc = new ComponentCollection
      val u = Select(0.25 -> 0.3, 0.25 -> 0.5, 0.25 -> 0.7, 0.25 -> 0.9)
      val f = Flip(u)
      val a = If(f, Select(0.3 -> 1, 0.7 -> 2), Constant(2))
      val pr = new Problem(cc, List(f))
      pr.add(u)
      pr.add(a)
      val cu = cc(u)
      val cf = cc(f)
      val ca = cc(a)
      cu.generateRange()
      cf.generateRange()
      ca.expand()
      ca.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()
      val result = multiplyAll(pr.solution)
      val fIndexT = cf.variable.range.indexOf(Regular(true))
      val fIndexF = cf.variable.range.indexOf(Regular(false))
      val pT = result.get(List(fIndexT))
      val pF = result.get(List(fIndexF))
      (pT / (pT + pF)) should be (0.6 +- 0.01)
    }

    "with a model using chain and no conditions or constraints, when the outcomes are at the top level, produce the correct answer" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      pr.add(e2)
      pr.add(e3)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()
      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      result.get(List(c4Index1)) should be ((0.3 * 0.1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and no conditions or constraints, when the outcomes are nested, produce the correct answer" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()
      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      result.get(List(c4Index1)) should be ((0.3 * 0.1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on the result, when the outcomes are at the top level, correctly condition the parent" in {
      Universe.createNew()
      val e1= Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e4.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e2)
      pr.add(e3)
      pr.add(e4)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be ((0.3 * 0.1 / (0.3 * 0.1 + 0.7 * 0.7)) +- 0.000000001)
    }

    "with a model using chain and a condition on the result, when the outcomes are nested, correctly condition the parent" in {
      Universe.createNew()
      val e1= Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e4.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e4)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be ((0.3 * 0.1 / (0.3 * 0.1 + 0.7 * 0.7)) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are at the top level, correctly condition the result" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      pr.add(e2)
      pr.add(e3)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      val c4Index2 = c4.variable.range.indexOf(Regular(2))
      val c4Index3 = c4.variable.range.indexOf(Regular(3))
      val p1 = result.get(List(c4Index1))
      val p2 = result.get(List(c4Index2))
      val p3 = result.get(List(c4Index3))
      (p1 / (p1 + p2 + p3)) should be ((0.3 * 1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are at the top level, " +
    "not change the belief about the parent" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)
      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e2)
      pr.add(e3)
      pr.add(e4)
      val c1 = cc(e1)
      val c2 = cc(e2)
      val c3 = cc(e3)
      val c4 = cc(e4)
      c1.generateRange()
      c2.generateRange()
      c3.generateRange()
      c4.expand()
      c4.generateRange()
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be (0.3 +- 0.01)
    }

    // These tests are invalid because nested evidence is disallowed
    /*"with a model using chain and a condition on one of the outcome elements, when the outcomes are nested, correctly condition the result" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e4))
      pr.add(e1)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c4Index1 = c4.variable.range.indexOf(Regular(1))
      val c4Index2 = c4.variable.range.indexOf(Regular(2))
      val c4Index3 = c4.variable.range.indexOf(Regular(3))
      val p1 = result.get(List(c4Index1))
      val p2 = result.get(List(c4Index2))
      val p3 = result.get(List(c4Index3))
      (p1 / (p1 + p2 + p3)) should be ((0.3 * 1 + 0.7 * 0.7) +- 0.000000001)
    }

    "with a model using chain and a condition on one of the outcome elements, when the outcomes are nested, " +
    "not change the belief about the parent" in {
      Universe.createNew()
      val e1 = Flip(0.3)
      val e2 = Select(0.1 -> 1, 0.9 -> 2)
      val e3 = Select(0.7 -> 1, 0.2 -> 2, 0.1 -> 3)
      val e4 = Chain(e1, (b: Boolean) => if (b) e2; else e3)
      e2.observe(1)

      val cc = new ComponentCollection
      val pr = new Problem(cc, List(e1))
      pr.add(e4)
      val c1 = cc(e1)
      val c4 = cc(e4)
      c1.generateRange()
      c4.expand()
      val c2 = cc(e2)
      val c3 = cc(e3)
      c2.generateRange()
      c3.generateRange()
      c4.generateRange()
      //c4.subproblems.values.foreach(new ConstantStrategy(_, marginalVariableElimination).execute())
      new VEBPStrategy(pr, structuredRaising, Double.NegativeInfinity, 100).execute()

      val result = multiplyAll(pr.solution)
      val c1IndexT = c1.variable.range.indexOf(Regular(true))
      val c1IndexF = c1.variable.range.indexOf(Regular(false))
      val pT = result.get(List(c1IndexT))
      val pF = result.get(List(c1IndexF))
      (pT / (pT + pF)) should be (0.3 +- 0.01)
    }
    */
  }

  "Choosing VE or BP on a loopy model" should {
    "choose VE with a high threshold and BP with a low threshold, resulting in different answers" in {
        // This model is loopy and BP produces a different result from VE
        Universe.createNew()
        val x11 = Constant(1)
        val x12 = Constant(2)
        val e11 = Flip(0.4)
        val e12 = If(e11, x11, x12)
        val e13 = Apply(e12, e11, (i: Int, b: Boolean) => if (b) i + 1 else i + 2)
        // e3 is 2 iff e1 is true, because then e2 is 1
        val cc1 = new ComponentCollection
        val pr1 = new Problem(cc1, List(e13))
        pr1.add(x11)
        pr1.add(x12)
        pr1.add(e11)
        pr1.add(e12)
        val cx11 = cc1(x11)
        val cx12 = cc1(x12)
        val c11 = cc1(e11)
        val c12 = cc1(e12)
        val c13 = cc1(e13)
        c11.generateRange()
        c12.expand()
        cx11.generateRange()
        cx12.generateRange()
        c12.generateRange()
        c13.generateRange()
        new VEBPStrategy(pr1, structuredRaising, Double.PositiveInfinity, 100).execute() // this should choose VE
        val result1 = multiplyAll(pr1.solution)

        Universe.createNew()
        val x21 = Constant(1)
        val x22 = Constant(2)
        val e21 = Flip(0.4)
        val e22 = If(e21, x21, x22)
        val e23 = Apply(e22, e21, (i: Int, b: Boolean) => if (b) i + 1 else i + 2)
        // e3 is 2 iff e1 is true, because then e2 is 1
        val cc2 = new ComponentCollection
        val pr2 = new Problem(cc2, List(e23))
        pr2.add(x21)
        pr2.add(x22)
        pr2.add(e21)
        pr2.add(e22)
        val cx21 = cc2(x21)
        val cx22 = cc2(x22)
        val c21 = cc2(e21)
        val c22 = cc2(e22)
        val c23 = cc2(e23)
        c21.generateRange()
        c22.expand()
        cx21.generateRange()
        cx22.generateRange()
        c22.generateRange()
        c23.generateRange()
        new VEBPStrategy(pr2, structuredRaising, Double.NegativeInfinity, 100).execute() // this should choose BP
        val result2 = multiplyAll(pr2.solution)

        val c13Index2 = c13.variable.range.indexOf(Regular(2))
        val p1 = result1.get(List(c13Index2))
        val c23Index2 = c23.variable.range.indexOf(Regular(2))
        val p2 = result2.get(List(c23Index2))
        p2 should not equal (p1)
    }
  }

  def multiplyAll(factors: List[Factor[Double]]): Factor[Double] = factors.foldLeft(Factory.unit(SumProductSemiring()))(_.product(_))

  class EC1 extends ElementCollection { }
}
