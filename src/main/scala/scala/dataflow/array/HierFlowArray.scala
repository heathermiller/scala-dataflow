package scala.dataflow.array

import scala.dataflow.Future
import scala.annotation.tailrec

class HierFlowArray[A : ClassManifest](
  private[array] val subData: Array[FlowArray[A]],
  private[array] val subSize: Int
) extends FlowArray[A] with FAJob.Observer {

  // Fields
  val size = subData.length * subSize

  // Calculation Information
  @volatile private[array] var srcJob: FAJob = null

  def map[B : ClassManifest](f: A => B): FlowArray[B] = null
  def flatMapN[B : ClassManifest](n: Int)(f: A => FlowArray[B]): FlowArray[B] = null
  def mutConverge(cond: A => Boolean)(it: A => Unit): FlowArray[A] = null
  def converge(cond: A => Boolean)(it: A => A): FlowArray[A] = null
  def fold[A1 >: A](z: A1)(op: (A1, A1) => A1): Future[A1] = null

  def blocking: Array[A] = null

  def done = subData.forall(_.done)

}
