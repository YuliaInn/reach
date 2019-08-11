package org.clulab.reach.context.context_exec

import java.io.{File, PrintWriter}

import ai.lum.nxmlreader.NxmlReader
import com.typesafe.config.ConfigFactory
import org.clulab.reach.PaperReader.{contextEngineParams, ignoreSections, preproc, procAnnotator}
import org.clulab.reach.ReachSystem
import org.clulab.reach.context.ContextEngineFactory.Engine
import org.clulab.reach.mentions.{BioEventMention, BioTextBoundMention}

object Polarity2ParallelRun extends App {
  val config = ConfigFactory.load()
  val papersDir = config.getString("polarityContext.temporaryRun2")
  val nxmlReader = new NxmlReader(ignoreSections.toSet, transformText = preproc.preprocessText)
  val contextEngineType = Engine.withName(config.getString("contextEngine.type"))
  lazy val reachSystem = new ReachSystem(processorAnnotator = Some(procAnnotator),
    contextEngineType = contextEngineType,
    contextParams = contextEngineParams)
  val fileListUnfiltered = new File(papersDir)
  val fileList = fileListUnfiltered.listFiles().filter(x => x.getName.endsWith(".nxml"))
  val egfDiffEvents = collection.mutable.ListBuffer[BioEventMention]()
  val dirForOutput = config.getString("polarityContext.contextLabelsOutputDir")
  var papersDone = 0
  for(file <- fileList) {
    try{
      val nxmlDoc = nxmlReader.read(file)
      val document = reachSystem.mkDoc(nxmlDoc)
      val mentions = reachSystem.extractFrom(document)
      val evtMentionsOnly = mentions.collect { case evt: BioEventMention => evt }
      for(ev <- evtMentionsOnly) {
        val sentenceID = ev.sentence
        val tokenInterval = ev.tokenInterval
        val subsentence = ev.document.sentences(sentenceID).words.slice(tokenInterval.start,tokenInterval.end+1).mkString(" ")
        if (checkAddingCondition(subsentence, ev))
        {egfDiffEvents += ev
          println(subsentence)}
      }
      papersDone += 1
      println(s"Papers done: ${papersDone}")
    }

    catch {
      case runtimeException: RuntimeException => {
        println(runtimeException)
        println(s"Skipping ${file.getName}")
      }
    }

  }

  val egfDiffEventsWithContext = egfDiffEvents.filter(_.hasContext())
  println(s"Number of events with associated context: ${egfDiffEventsWithContext.size}")

  for(e <- egfDiffEventsWithContext) {
    val docID = e.document.id match {
      case Some(x) => s"PMC${x.split("_")(0)}"
      case None => "unknown"
    }

    val path = dirForOutput.concat(docID)
    val paperIDDir = new File(path)
    if(!paperIDDir.exists()) {
      paperIDDir.mkdirs()
    }

    var polarity = "unknown"
    if(e.label.contains("Positive")) polarity = "activation"
    else if(e.label.contains("Negative")) polarity = "inhibition"
    val eventFileName = path.concat(s"/ContextsForEvent_${extractEvtId(e)}_${polarity}.txt")
    val eventFile = new File(eventFileName)
    if (!eventFile.exists()) {
      eventFile.createNewFile()
    }
    val contextLabels = collection.mutable.ListBuffer[String]()
    val map = e.context match {
      case Some(x) => x
      case None => Map.empty
    }

    for((_, labelSeq) <- map) {
      contextLabels ++= labelSeq
    }

    val stringToWrite = contextLabels.mkString(",")
    val printWriterPerEvent = new PrintWriter(eventFile)
    printWriterPerEvent.write(stringToWrite)
    printWriterPerEvent.close()

  }

  def checkAddingCondition(sentence: String, event:BioEventMention):Boolean = {
    checkEGFcase(sentence) && checkDifferentCase(sentence) && checkValidPolarity(event)
  }

  def checkEGFcase(sentence:String):Boolean = {
    sentence.contains("EGF") || sentence.contains("egf") || sentence.contains("Epidermal Growth Factor") || sentence.contains("Epidermal growth factor") || sentence.contains("epidermal growth factor")
  }

  def checkDifferentCase(sentence:String):Boolean = {
    sentence.contains("differentiation") || sentence.contains("Differentiation") || sentence.contains("cell differentiation") || sentence.contains("Cell differentiation") || sentence.contains("cell-differentiation")
  }

  def checkValidPolarity(evt:BioEventMention):Boolean = {
    evt.label.contains("Positive") || evt.label.contains("Negative")
  }

  type Pair = (BioEventMention, BioTextBoundMention)
  type EventID = String
  type ContextID = (String, String)
  def extractEvtId(evt:BioEventMention):EventID = {
    val sentIndex = evt.sentence
    val tokenIntervalStart = (evt.tokenInterval.start).toString()
    val tokenIntervalEnd = (evt.tokenInterval.end).toString()
    sentIndex+tokenIntervalStart+tokenIntervalEnd
  }




}
