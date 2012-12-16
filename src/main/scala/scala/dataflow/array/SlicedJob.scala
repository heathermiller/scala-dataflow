package scala.dataflow.array

trait SlicedJob {

  type SliceDep = Option[(IndexedSeq[FAJob], Boolean)]

  private[array] def sliceJobs(from: Int, to: Int): SliceDep

}
