package manticore.compiler.assembly.levels.unconstrained

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import manticore.compiler.AssemblyContext
import manticore.compiler.Color
import manticore.compiler.CyclicColorGenerator
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.BinaryOperator.AND
import manticore.compiler.assembly.BinaryOperator.BinaryOperator
import manticore.compiler.assembly.BinaryOperator.OR
import manticore.compiler.assembly.BinaryOperator.XOR
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.CanCollectProgramStatistics
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.UInt16
import org.jgrapht.Graph
import org.jgrapht.GraphMapping
import org.jgrapht.Graphs
import org.jgrapht.alg.util.UnionFind
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.AsUnmodifiableGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.traverse.TopologicalOrderIterator

import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import scala.collection.immutable.{BitSet => Cut}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.{HashMap => MHashMap}
import scala.collection.mutable.{HashSet => MHashSet}
import scala.jdk.CollectionConverters._
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.placed.Helpers.DeadCode

trait EmitWidthStatistics extends DependenceGraphBuilder with UnconstrainedIRTransformer with CanCollectProgramStatistics {

  val flavor = UnconstrainedIR
  import flavor._

  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    assert(program.processes.size == 1, s"Expected 1 process, but found ${program.processes.size}")

    val proc = program.processes.head

    // Does not change the program, just prints statistics, so we can
    // return the original program.
    program
  }

}

object EmitWidthStatistics extends EmitWidthStatistics
