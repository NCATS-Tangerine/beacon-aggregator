package bio.knowledge.aggregator;

public interface Query<Q> {

	/**
	 * 
	 * @param query
	 * @return
	 */
	public String makeQueryString();

	/**
	 * 
	 * @return
	 */
	public int makeThreshold();
}
