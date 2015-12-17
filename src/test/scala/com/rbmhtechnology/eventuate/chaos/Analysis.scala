package com.rbmhtechnology.eventuate.chaos

import scala.collection.immutable.Seq
import scala.io.Source

object Analysis extends App {
  case class Record(logId: String = "", writeType: String = "", sequenceNr: Long = -1L, emitterId: String = "", emitterCtr: Int = -1, delta: Int = 0)

  private val UpdateOpPattern =
    """^UpdateOp\((-?\d+)\)""".r

  def ar = args(0)

  def delta(operation: String): Int = operation match {
    case UpdateOpPattern(d) => d.toInt
    case _ => throw new Exception(s"unknown operation $operation")
  }

  def record(write: String): Record = {
    val elems = write.split(" ")
    Record(elems(0), elems(1), elems(2).toLong, elems(3), elems(4).toInt, delta(elems(5)))
  }

  def writes(file: String): Seq[String] = {
    Source.fromFile(file).getLines().filter(_.startsWith("-->")).map(_.substring(4)).toVector
  }

  def records(file:String): Seq[Record] =
    writes(file).map(record)

  val all = List(
    records(s"results/r${ar}-location1.txt"),
    records(s"results/r${ar}-location2.txt"),
    records(s"results/r${ar}-location3.txt"))

  val writes =
    all.map(_.filterNot(_.writeType == "dmp"))

  val dumps =
    all.map(_.filter(_.writeType == "dmp"))

  println()
  println("--- Counters (from writes) ---")
    writes.map(_.map(_.delta).sum).foreach(println)

  println()
  println("--- Counters (from dumps) ---")
  dumps.map(_.map(_.delta).sum).foreach(println)

  def compareRecords(i: Int, j: Int): Unit = {
    val li = s"location${i+1}"
    val lj = s"location${j+1}"

    println()
    println(s"--- Records comparison $li - $lj ---")
    writes(i).filter(_.emitterId == li).zipAll(writes(j).filter(_.emitterId == li), Record(), Record()) foreach {
      case (r1, r2) if r1.emitterCtr == r2.emitterCtr => println(f"  $r1%-55s $r2%-55s")
      case (r1, r2)                                   => println(f"! $r1%-55s $r2%-55s")
    }
  }

  def compareLogs(i: Int, j: Int): Unit = {
    val li = s"location${i+1}"
    val lj = s"location${j+1}"

    println()
    println(s"--- Logs comparison $li - $lj ---")
    dumps(i).filter(_.emitterId == li).zipAll(dumps(j).filter(_.emitterId == li), Record(), Record()) foreach {
      case (r1, r2) if r1.emitterCtr == r2.emitterCtr => println(f"  $r1%-55s $r2%-55s")
      case (r1, r2)                                   => println(f"! $r1%-55s $r2%-55s")
    }
  }

  def compareRecordsWithLog(i: Int): Unit = {
    val li = s"location${i+1}"

    println()
    println(s"--- Records - log comparison $li ---")
    writes(i).filter(_.emitterId == li).zipAll(dumps(i).filter(_.emitterId == li), Record(), Record()) foreach {
      case (r1, r2) if r1.emitterCtr == r2.emitterCtr => println(f"  $r1%-55s $r2%-55s")
      case (r1, r2)                                   => println(f"! $r1%-55s $r2%-55s")
    }
  }

  def detectReps(i: Int): Unit = {
    val li = s"location${i+1}"

    println()
    println(s"--- Reps $li ---")
    writes(i).filter(w => w.writeType == "rep" && w.emitterId == li).foreach(println)
  }

  0 to 2 foreach { i =>
    val j = (i+1) % 3
    val k = (i+2) % 3
    compareRecords(i, j)
    compareRecords(i, k)
  }

  0 to 2 foreach { i =>
    val j = (i+1) % 3
    val k = (i+2) % 3
    compareLogs(i, j)
    compareLogs(i, k)
  }

  0 to 2 foreach { i =>
    compareRecordsWithLog(i)
  }

  0 to 2 foreach { i =>
    detectReps(i)
  }
}
