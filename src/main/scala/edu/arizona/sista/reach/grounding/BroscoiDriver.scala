package edu.arizona.sista.reach.grounding

import java.io._

import edu.arizona.sista.odin._
import edu.arizona.sista.processors.{Document, DocumentSerializer}
import edu.arizona.sista.reach.ReachSystem
import edu.arizona.sista.reach.mentions._
import org.slf4j.LoggerFactory

/**
  * Top-level driver to output grounded text mentions in Broscoi format.
  *   Written by Tom Hicks. 4/13/2015.
  *   Last Modified: Update to use new display label.
  */
object BroscoiDriver extends App {
  private val idCntr = new IncrementingCounter() // counter sequence class

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  val ds = new DocumentSerializer
  val reach = new ReachSystem

  val PapersDir = s"${System.getProperty("user.dir")}/src/test/resources/inputs/papers/"
  val paperNames = Seq(
//    "MEKinhibition.txt.ser",
//    "UbiquitinationofRas.txt.ser",
//    "PMC3441633.txt.ser",
    "PMC3847091.txt.ser"
  )

  def cleanText (m: Mention): String = {
    """(\s+|\n|\t|[;])""".r.replaceAllIn(m.document.sentences(m.sentence).getSentenceText, " ")
  }

  def docFromSerializedFile (filename: String): Document = {
    val br = new BufferedReader(new FileReader(filename))
    val doc = ds.load(br)
    doc
  }

  def getText(fileName: String):String = scala.io.Source.fromFile(fileName).mkString

  // val outDir = s"${System.getProperty("java.io.tmpdir")}" + File.separator
  val outDir = s"${System.getProperty("user.dir")}" + File.separator
  def mkOutputName (paper:String, ext:String): String = {
    outDir + {"""^.*?/|.txt.ser""".r.replaceAllIn(paper, "")} + ext
  }

  def processPapers (papers:Seq[String]): Unit = {
    papers.foreach { paper => processPaper(paper) }
  }

  def processPaper (paper: String): Unit = {
    val outName = mkOutputName(paper, ".bros")
    val outFile = new FileOutputStream(new File(outName))
    val inFile = s"$PapersDir/$paper"

    val doc = paper match {
      case ser if ser.endsWith("ser") => docFromSerializedFile(inFile)
      case _ => reach.mkDoc(getText(inFile), "broscoidriver")
    }

    val mentions = reach.extractFrom(doc)
    val sortedMentions = mentions.sortBy(m => (m.sentence, m.start)) // sort by sentence, start idx
    outputTextBoundMentions(sortedMentions, doc, outFile)
  }

  /** Ground, then output the given sequence of mentions. */
  def outputTextBoundMentions (mentions:Seq[BioMention], doc:Document, fos:FileOutputStream): Unit = {
    val out:PrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos)))
    val ground = new LocalGrounder()
    val tbMentions = mentions.filter(_.isInstanceOf[TextBoundMention])
    val state = State.apply(tbMentions)
    val groundedMentions = ground.apply(tbMentions, state)
    groundedMentions.foreach { case m: BioMention =>
      val ns = if (m.xref.isDefined) m.xref.get.namespace else ""
      val id = if (m.xref.isDefined) m.xref.get.id else ""
      out.println(s"T${idCntr.next()}\t${m.displayLabel}\t${m.startOffset}\t${m.endOffset}\t${ns}\t${id}\t${m.text}")
    }
    out.flush()
    out.close()
  }

  // Top-level Main of script:
  processPapers(paperNames)
}