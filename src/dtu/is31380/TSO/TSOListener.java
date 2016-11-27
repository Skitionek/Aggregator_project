package dtu.is31380.TSO;

public interface TSOListener {

  public enum RegulationType {
    UP,  //Decrease consumption ("Up" is generation increase or consumption decrease)
    DOWN //Increase consumption
  };
  
  /**
    * Called by the TSO to ask if the aggregator could deliver the specified service in the future.
    * @param startTime The beginning of the requested service period, in seconds since simulation start
    * @param endTime The end of the requested service period, in seconds since simulation start
    * @param maxDelay The maximum time delay between activation and power regulation tolerated
    *                 by the aggregator (in seconds)
    * @param minDuration The minimum length during which the regulation must be performed, after
    *                 activation (in seconds)
    * @param regType Enum value specifying whether the request is for up- or downregulation.
    * @return The absolute magnitude (maximum size of regulation delta) the aggregator commits to
    *         deliver (in kW). This value should be positive if a power increase has been requested,
    *         negative if a power decrease has been requested, or zero if the aggregator cannot
    *         or does not want to fulfill the request at all.  
    */
  
  double serviceRequest(long startTime, long endTime,
                        double maxDelay, double minDuration, RegulationType regType);
  
  /**
   * Called by the TSO to trigger the delivery of a service which TSO and aggregator have agreed
   * upon earlier (as part of a serviceRequest). 
   * @param regType Enum value specifying whether the request is for up- or downregulation.
   *                Note that the aggregator should not comply with a request type which
   *                has not been agreed on (i.e. aggregator agreed to upregulate, TSO activates
   *                a downregulation service).
   * @param magnitude The magnitude (regulation delta) to be delivered/activated. The aggregator
   *                  should be prepared to deliver (approximately) any regulation value within
   *                  the boundaries of the agreement (i.e. between zero and the value returned
   *                  to the TSO during the serviceRequest call).
   */
  
  void serviceActivation(RegulationType regType, double magnitude);
  
}
