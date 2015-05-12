package edu.arizona.sista.bionlp

import java.io.File
import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.{ FileUtils, FilenameUtils }
import edu.arizona.sista.odin._
import edu.arizona.sista.bionlp.mentions._
import edu.arizona.sista.odin.extern.export.reach._

object RunSystem extends App {
  // use specified config file or the default one if one is not provided
  val config =
    if (args.isEmpty) ConfigFactory.load()
    else ConfigFactory.parseFile(new File(args(0))).resolve()

  val nxmlDir = new File(config.getString("nxmlDir"))
  val friesDir = new File(config.getString("friesDir"))
  val encoding = config.getString("encoding")
  val outputType = config.getString("outputType")

  // if nxmlDir does not exist there is nothing to do
  if (!nxmlDir.exists) {
    sys.error(s"${nxmlDir.getCanonicalPath} does not exist")
  }

  // if friesDir does not exist create it
  if (!friesDir.exists) {
    println(s"creating ${friesDir.getCanonicalPath}")
    FileUtils.forceMkdir(friesDir)
  } else if (!friesDir.isDirectory) {
    sys.error(s"${friesDir.getCanonicalPath} is not a directory")
  }

  println("initializing reach ...")
  val reach = new ReachSystem

  println("initializing nxml2fries ...")
  val nxml2fries = new Nxml2Fries(
    config.getString("nxml2fries.executable"),
    config.getBoolean("nxml2fries.removeCitations"),
    config.getStringList("nxml2fries.ignoreSections").asScala.toSet,
    encoding)

  // process papers in parallel
  for (file <- nxmlDir.listFiles.par if file.getName.endsWith(".nxml")) {
    val paperId = FilenameUtils.removeExtension(file.getName)

    // process individual sections and collect all mentions
    val paperMentions = for {
      entry <- nxml2fries.extractEntries(file)
      mention <- reach.extractFrom(entry)
    } yield mention

    if (outputType != "text") {             // if reach will handle output
      outputMentions(paperMentions, outputType, paperId, friesDir)
    }
    else {                                  // else dump all paper mentions to file
      val lines = paperMentions.flatMap(mentionToStrings)
      val outFile = new File(friesDir, s"$paperId.txt")
      println(s"writing ${outFile.getName} ...")
      FileUtils.writeLines(outFile, lines.asJavaCollection)
    }
  }

  def outputMentions(mentions:Seq[BioMention], outputType:String, paperId:String, outputDir:File) = {
    val outFile = new File(outputDir, s"${paperId}.json")
    val outputter = new ReachOutput()
    println(s"writing ${outFile.getName} ...")
    outputter.toJSON(mentions, outFile)
  }

  /** Generates a representation of the given mention as a list of strings. */
  def mentionToStrings(mention: Mention): List[String] = {
    return mentionToStrings(mention, 0)
  }

  /** Return a list of strings representing the given mention at the given indentation level. */
  private def mentionToStrings(mention: Mention, level: Integer): List[String] = {
    import scala.collection.mutable.MutableList
    val mStrings: MutableList[String] = MutableList[String]()
    val indent = ("  " * level)
    mention match {
      case mention: BioTextBoundMention =>
        mStrings += s"${indent}TextBoundMention: [S${mention.sentence}]: ${mention.label}"
        mStrings += s"${indent}text: ${mention.text}"
        if (mention.isGrounded)
          mStrings += s"${indent}xref: ${mention.xref.get}"
        if (level == 0) mStrings += ("=" * 80)
      case mention: BioEventMention =>
        mStrings += s"${indent}EventMention: [S${mention.sentence}]: ${mention.label}"
        mStrings += s"${indent}text: ${mention.text}"
        mStrings += s"${indent}trigger:"
        mStrings ++= mentionToStrings(mention.trigger, level+1)
        mention.arguments foreach {
          case (k,vs) => {
            mStrings += s"${indent}${k} (${vs.length}):"
            for (v <- vs) {
              mStrings ++= mentionToStrings(v, level+1)
            }
          }
        }
        if (level == 0) mStrings += ("=" * 80)
      case mention: BioRelationMention =>
        mStrings += s"${indent}RelationMention: [S${mention.sentence}]: ${mention.label}"
        mStrings += s"${indent}text: ${mention.text}"
        mention.arguments foreach {
          case (k,vs) => {
            mStrings += s"${indent}${k} (${vs.length}):"
            for (v <- vs) {
              mStrings ++= mentionToStrings(v, level+1)
            }
          }
        }
        if (level == 0) mStrings += ("=" * 80)
      case _ => ()
    }
    return mStrings.toList
  }

}
