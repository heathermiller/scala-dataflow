package scala.dataflow.array

/**
 * any job which is sliced and jobs for a sub-slice may be queried.
 */ 
private[array] trait SlicedJob {

  import SlicedJob._

  /**
   * slice-wise dependencies
   *
   * calculates the jobs responsible for the given slice.
   * @return `None`, if the slice is completed. `Some((jobs, bool))`,
   * if the slice is not yet completed, where `jobs` is the jobs
   * required for completion, `bool` indicates whether this SlicedJob
   * has to be queried again after all the jobs in `jobs`
   * complete. Note that `jobs` may not be empty (should return `None`
   * in this case)
   */
  private[array] def sliceJobs(from: Int, to: Int): SliceDep

}

private[array] object SlicedJob {
  /**
   * Information about slice dependencies.
   * 
   * Invariant of this type:
   *   Some((js, _)) ==> !js.isEmpty
   */
  type SliceDep = Option[(IndexedSeq[FAJob], Boolean)]

  /**
   * Merges multiple slice dependencies
   */
  def mergeDeps(d: SliceDep*) = {
    val (js, redo) = d.flatten.unzip
    val fjs = js.flatten
    if (fjs.isEmpty) None
    else Some(fjs.toIndexedSeq, redo.exists(x => x))
  }
}
