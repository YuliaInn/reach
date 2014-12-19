package edu.arizona.sista.bionlp.reach.brat

import java.io.{File, InputStream}
import scala.collection.mutable.HashMap
import edu.arizona.sista.struct.Interval
import edu.arizona.sista.processors.{Document, Sentence}
import edu.arizona.sista.matcher.{Mention, EventMention, TextBoundMention}
import edu.arizona.sista.bionlp.reach.core.RelationMention

object Brat {
  def readStandOff(input: String): Seq[Annotation] =
    input.lines.toSeq flatMap parseAnnotation

  def readStandOff(input: InputStream): Seq[Annotation] =
    io.Source.fromInputStream(input).getLines.toSeq flatMap parseAnnotation

  def readStandOff(input: File): Seq[Annotation] =
    io.Source.fromFile(input).getLines.toSeq flatMap parseAnnotation

  def parseAnnotation(line: String): Option[Annotation] = {
    val chunks = line.trim.split("\t")
    val elems = chunks(1).split(" ")

    def arguments(elems: Seq[String]): Map[String, Seq[String]] =
      elems map (_.split(":")) groupBy (_(0)) mapValues(_.map(_(1)))

    chunks.head match {
      // text bound annotation
      case id if id.startsWith("T") =>
        val Array(label, offsets) = chunks(1).split(" ", 2)
        val spans = offsets.split(";") map (_.split(" ").map(_.toInt)) map (t => Interval(t(0), t(1)))
        Some(TextBound(id, label, spans, chunks(2)))

      // relation
      case id if id.startsWith("R") =>
        val label = elems.head
        val args = arguments(elems.tail)
        Some(Relation(id, label, args))

      // event
      case id if id.startsWith("E") =>
        val Array(label, trigger) = elems.head.split(":")
        val args = arguments(elems.tail)
        Some(Event(id, label, trigger, args))

      // equivalence
      case id if id.startsWith("*") =>
        Some(Equivalence(id, elems.head, elems.tail))

      // attribute
      case id if id.startsWith("A") || id.startsWith("M") =>
        if (elems.size == 2) {
          Some(BinaryAttribute(id, elems(0), elems(1)))
        } else if (elems.size == 3) {
          Some(MultiValueAttribute(id, elems(0), elems(1), elems(2)))
        } else {
          sys.error("unrecognized attribute type")
        }

      case id if id.startsWith("N") =>
        val Array(resource, entry) = elems(2).split(":")
        Some(Normalization(id, elems(0), elems(1), resource, entry, chunks(2)))

      // ignore everything else
      case _ => None
    }
  }

  def alignLabels(document: Document, annotations: Seq[Annotation]): Seq[Seq[String]] = {
    val textBound = annotations filter (_.isInstanceOf[TextBound]) map (_.asInstanceOf[TextBound])
    document.sentences map (alignSentenceLabels(_, textBound))
  }

  def alignTokenLabel(sentence: Sentence, token: Interval, annotations: Seq[TextBound]): String = {
    var label = "O"
    for (a <- annotations; span <- a.spans) {
      if (token intersects span) {
        if (token.start <= span.start) {
          label = s"B-${a.label}"
        } else {
          label = s"I-${a.label}"
        }
      }
    }
    label
  }

  def alignSentenceLabels(sentence: Sentence, annotations: Seq[TextBound]): Seq[String] = {
    sentence.startOffsets zip sentence.endOffsets map {
      case (start, end) => alignTokenLabel(sentence, Interval(start, end), annotations)
    }
  }

  def dumpStandoff(mentions: Seq[Mention], doc: Document): String =
    dumpStandoff(mentions, doc, Nil)

  def dumpStandoff(mentions: Seq[Mention], doc: Document, annotations: Seq[Annotation]): String = {
    val idTracker = IdTracker(annotations)
    mentions.map(m => dumpStandoff(m, doc, idTracker)).mkString("\n")
  }

  def dumpStandoff(mention: Mention, doc: Document, tracker: IdTracker): String = {
    val sentence = doc.sentences(mention.sentence)
    def getId(m: Mention): String = m match {
      case m: TextBoundMention => tracker.getId(m, doc)
      case m: EventMention => tracker.getId(m, doc)
      case m: RelationMention => tracker.getId(m, doc)
    }

    def displayRuleName(m: Mention): String = {
      //example:
      //#10     Origin E4       Rulename1
      s"#${getId(m).drop(1)}\tOrigin ${getId(m)}\t${m.foundBy}"
    }

    mention match {
      case m: TextBoundMention =>
        val offsets = s"${sentence.startOffsets(m.start)} ${sentence.endOffsets(m.end - 1)}"
        val str = sentence.words.slice(m.start, m.end).mkString(" ")
        s"${getId(m)}\t${m.label} $offsets\t$str\n${displayRuleName(m)}"
      case m: EventMention =>
        val trigger = getId(m.trigger)
        val arguments = m.arguments.flatMap{ case (name, vals) => vals map (v => s"$name:${getId(v)}") }.mkString(" ")
        s"${getId(m)}\t${m.label}:$trigger $arguments\n${displayRuleName(m)}"

      case m: RelationMention =>
        val arguments = m.arguments.flatMap{ case (name, vals) => vals map (v => s"$name:${getId(v)}") }.mkString(" ")
        s"${getId(m)}\tOrigin $arguments\n${displayRuleName(m)}"
    }
  }

  def syntaxStandoff(doc: Document): String = {
    val idTracker = IdTracker()
    val tags = (doc.sentences.zipWithIndex flatMap {
      case (s, i) => s.tags.get.zipWithIndex map {
        case (tag, j) => (i,j) -> new TextBoundMention(tag, Interval(j), i, doc, "syntax")
      }
    }).toMap

    val tbIds = tags map { case (k,v) => k -> idTracker.getId(v, doc) }

    var id = 0
    val rels = doc.sentences.zipWithIndex flatMap {
      case (s, i) =>
        val outgoing = s.dependencies.get.outgoingEdges
        0 until outgoing.size flatMap {
          j => outgoing(j) map {
            case (k, dep) =>
              id += 1
              s"R$id\t$dep governor:${tbIds((i,j))} dependent:${tbIds((i,k))}"
          }
        }
    }

    (tags.values.map(m => dumpStandoff(m, doc, idTracker)) ++ rels).mkString("\n")
  }
}


class IdTracker(val textBoundLUT: HashMap[String, Interval]) {
  val mentionLUT = new HashMap[Mention, String]

  def getId(mention: TextBoundMention, doc: Document): String = {
    val span = charInterval(mention, doc)
    val ids = textBoundLUT.keys filter (id => textBoundLUT(id) intersects span)
    if (ids.size == 1) ids.head
    else if (ids.size > 1) ids.head  // arbitrarily returning the first one
    else {
      val id = s"T${textBoundLUT.size + 1}"
      val kv = (id -> span)
      textBoundLUT += kv
      id
    }
  }

  def getId(mention: EventMention, doc: Document): String =
    mentionLUT.getOrElseUpdate(mention, s"E${mentionLUT.size + 1}")

  def getId(mention: RelationMention, doc: Document): String =
    mentionLUT.getOrElseUpdate(mention, s"R${mentionLUT.size + 1}")

  def charInterval(mention: Mention, document: Document): Interval =
    charInterval(mention, document.sentences(mention.sentence))

  def charInterval(mention: Mention, sentence: Sentence): Interval = {
    val charStart = sentence.startOffsets(mention.start)
    val charEnd = sentence.endOffsets(mention.end - 1)
    Interval(charStart, charEnd)
  }
}

object IdTracker {
  def apply(): IdTracker = apply(Nil)

  def apply(annotations: Seq[Annotation]): IdTracker = {
    val lut = annotations flatMap {
      _ match {
        case a: TextBound => Some(a.id -> a.totalSpan)
        case _ => None
      }
    }
    new IdTracker(HashMap(lut: _*))
  }
}
