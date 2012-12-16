package scala.dataflow.array

trait SlicedJob {

  // Invariant of this type:
  //   Some((js, _)) ==> !js.isEmpty
  type SliceDep = Option[(IndexedSeq[FAJob], Boolean)]

  private[array] def sliceJobs(from: Int, to: Int): SliceDep

}
