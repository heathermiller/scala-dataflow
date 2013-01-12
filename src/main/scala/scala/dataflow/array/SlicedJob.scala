package scala.dataflow.array

private[array] trait SlicedJob {

  import SlicedJob._

  /**
   * slice-wise dependencies
   *
   * calculates the jobs responsible for the given slice. See
   * `SliceDep` for documentation of the return type.
   */
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
