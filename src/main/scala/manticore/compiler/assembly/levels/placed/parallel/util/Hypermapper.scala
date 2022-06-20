package manticore.compiler.assembly.levels.placed.parallel.util

import java.nio.file.Files
import java.io.PrintWriter
import java.nio.file.Path
import java.io.BufferedReader

import scala.sys.process.Process
import scala.sys.process.ProcessBuilder
import scala.sys.process.ProcessIO
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import manticore.compiler.AssemblyContext
import java.io.FileNotFoundException

case class HyperMapperConfig(numCores: Int, numProcesses: Int, optIterations: Int = 10, numWarmUpSamples: Int = 4) {

  def indexOf(k: String) = k.drop(1).toInt
  val objective          = "vcycle"
  def asJson: String = {

    def intParam(i: Int) = {
      s"\"p$i\": { \"parameter_type\": \"integer\", \"values\": [0, ${numCores - 1}] }"
    }

    val params = Seq.tabulate(numProcesses) { intParam }.mkString(",\n")
    s"""|{
            |   "application_name": "manticore",
            |   "hypermapper_mode": {
            |     "mode": "client-server"
            |   },
            |   "design_of_experiment" : {
            |     "doe_type": "standard latin hypercube",
            |     "number_of_samples": ${numWarmUpSamples}
            |   },
            |   "feasible_output" : {
            |       "enable_feasible_predictor": true,
            |       "false_value": "${HyperMapperConfig.falseValue}",
            |       "true_value" : "${HyperMapperConfig.trueValue}"
            |   },
            |   "optimization_iterations": ${optIterations},
            |   "optimization_objectives": [ "${objective}" ],
            |   "optimization_method": "bayesian_optimization",
            |   "input_parameters": {
            |       ${params}
            |   },
            |   "models": { "model": "random_forest", "number_of_trees": 20 }
            |
            |
            |}
            """.stripMargin

  }

  def dumpJson(path: Path): Unit = {
    val writer = new PrintWriter(path.toFile)
    writer.println(asJson)
    writer.flush()
    writer.close()
  }
}
object HyperMapperConfig {

  val falseValue: String = "0"
  val trueValue: String  = "1"
  val validName: String  = "Valid"

}

object HyperMapper {

  def apply(
      cfg: HyperMapperConfig,
      hmHome: String = scala.util.Properties.envOrElse("HYPERMAPPER_HOME", "hypermapper")
  ): HyperMapper = {

    val cfgPath = Files.createTempFile("hypermapper", "config.json")
    cfg.dumpJson(cfgPath)
    val wordDir    = Files.createTempDirectory("hypermapper")
    val scriptPath = Path.of(hmHome).resolve("scripts").resolve("hypermapper.py")
    if (scriptPath.toFile().exists()) {
      val hmCmd = Seq("python3", "scripts/hypermapper.py", cfgPath.toAbsolutePath.toString())

      val hmProcess = Process(
        command = hmCmd,
        cwd = Path.of(hmHome).toFile
      )

      new HyperMapper(hmProcess)
    } else {
      throw new FileNotFoundException(s"Could not locate HyperMapper script at ${scriptPath.toAbsolutePath()}")
    }

  }
}

class HyperMapper(private val process: ProcessBuilder) {

  private val pipeSize = 4096
  private var running: Boolean = false

  // the pipe that the client should use to write to HyperMapper's pipe input
  private val userInputStream = new PipedOutputStream()

  // the pipe from which a user should read HyperMapper's output
  private val userOutputStream = new PipedInputStream()

  // the error pipe from HyperMapper
  private val userErrStream = new PipedInputStream()

  /**
      * Method to attach to the HyperMapper's process input stream
      *
      * @param hmIn output stream from HyperMapper client to HyperMappers' input pipe
      */
  private def inputWriter(hmIn: OutputStream): Unit = {
    val istream   = new PipedInputStream(userInputStream)
    val bytes     = Array.fill(pipeSize)(0.toByte)
    var bytesRead = 0
    while (bytesRead >= 0) {
      bytesRead = istream.read(bytes)
      if (bytesRead > 0) {
        hmIn.write(bytes, 0, bytesRead)
        hmIn.flush()
      }
    }
    istream.close()
    hmIn.flush()
    hmIn.close()
  }

  private def outputReader(hmOut: InputStream): Unit = {
    val ostream = new PipedOutputStream(userOutputStream)
    // println("HypperMapper out")
    val bytes     = Array.fill(pipeSize) { 0.toByte }
    var bytesRead = 0
    while (bytesRead >= 0) {
      bytesRead = hmOut.read(bytes)
      if (bytesRead > 0) {
        ostream.write(bytes, 0, bytesRead)
        ostream.flush()

      }
    }
    ostream.flush()
    ostream.close()
    hmOut.close()
  }

  private def errReader(errOut: InputStream): Unit = {
    val errStream = new PipedOutputStream(userErrStream)

    val bytes     = Array.fill(pipeSize) { 0.toByte }
    var bytesRead = 0
    while (bytesRead >= 0) {
      bytesRead = errOut.read(bytes)
      if (bytesRead > 0) {
        errStream.write(bytes, 0, bytesRead)
      }
    }

    errOut.close()
  }

  /**
    * Start HyperMapper and return reader/writer objects to communicate with it
    */
  def start() = {
    if (!running) {
      val io            = new ProcessIO(inputWriter, outputReader, errReader)
      val userReader    = new BufferedReader(new InputStreamReader(userOutputStream))
      val userWriter    = new BufferedWriter(new OutputStreamWriter(userInputStream))
      val userErrReader = new BufferedReader(new InputStreamReader(userErrStream))

      process run io
      running = true
      (userReader, userWriter, userErrReader)
    } else {
      throw new UnsupportedOperationException("Can not re-start HyperMapper!")
    }
  }

  def finish(): Unit = {
    if (running) {
      userInputStream.close()
      userOutputStream.close()
      userErrStream.close()
      running = false
    }
  }

}

class Client(reader: BufferedReader, writer: BufferedWriter) extends Runnable {
    override def run(): Unit = {

      var running = true
      var iter    = 0
      while (running) {

        println("Trying to read request")
        val rline = reader.readLine().trim()

        if (rline == "Your command?") {
          if (iter == 4) {
            writer.write("END\n")
            writer.flush()
          } else {
            println(s"Writing ${iter}")
            writer.write(s"$iter \n")
            writer.flush()
            iter += 1
          }
        } else if (rline == "DONE") {
          println("Finished!")
          running = false
        } else {
          print(s"READ: ${rline}")
        }
      }
    }
  }
object MyProcessTester extends App {

  val hm = new HyperMapper(
    Process(
      command = Seq("python3", "server.py")
    )
  )

  val (reader, writer, err) = hm.start()



  val client = new Client(reader, writer)
  val clientRunner = new Thread(client)
  clientRunner.start()
  clientRunner.join()

}
