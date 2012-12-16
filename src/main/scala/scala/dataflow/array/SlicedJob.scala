package scala.dataflow.array

private[array] trait SlicedJob {

  import SlicedJob._

  private[array] def sliceJobs(from: Int, to: Int): SliceDep

}

object SlicedJob {
  // Invariant of this type:
  //   Some((js, _)) ==> !js.isEmpty
  type SliceDep = Option[(IndexedSeq[FAJob], Boolean)]

  def mergeDeps(d: SliceDep*) = {
    val (js, redo) = d.flatten.unzip
    val fjs = js.flatten
    if (fjs.isEmpty) None
    else Some(fjs.toIndexedSeq, redo.exists(x => x))
  }
}
