package manticore.integration.thyrio



import java.io.File
import scala.sys.process.ProcessLogger

trait ExternalTool {

  import scala.sys.process._
  val name: String
  def stdPrint(x: String): Unit = println(x)
  def invoke(args: Seq[String], work_dir: File)(
      capture: String => Unit = stdPrint
  ): Unit = {
    val cmd = s"${name} ${args mkString (" ")}"
    val ret = sys.process
      .Process(
        command = cmd,
        cwd = work_dir
      )
      .!(ProcessLogger(line => capture(line)))
    if (ret != 0) {
      throw new RuntimeException(s"${name} returned ${ret}")
    }

  }
  def installed(): Boolean = s"which ${name}".! == 0
}

case object Make extends ExternalTool {
  override val name: String = "make"
}

case object Python3 extends ExternalTool {
    val name: String = "python3"
}

case object Verilator extends ExternalTool  {
    val name: String = "verilator"
}

case object  ThyrioFrontend extends ExternalTool {
    val name: String = "thyrio_frontend"
}



